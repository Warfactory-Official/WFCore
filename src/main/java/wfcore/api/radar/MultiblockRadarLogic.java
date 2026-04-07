package wfcore.api.radar;

import gregtech.api.GTValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.metatileentity.MetaTileEntity;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import wfcore.WFCore;
import wfcore.api.util.math.ClusterData;
import wfcore.api.util.math.IntCoord2;
import wfcore.api.util.math.BoundingBox;
import wfcore.common.managers.RadarDataManager;
import wfcore.common.managers.RadarSavedData;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityRadar;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static wfcore.api.util.LogUtil.logExceptionWithTrace;

public class MultiblockRadarLogic {
    //TODO: Make those values adjustable in GUI
    public static final int MIN_PTS = 10;
    public static final int EPS = 200;

    private int voltageTier;
    private int overclockAmount;
    @Nullable
    public UUID lastScan = null;
    private final MetaTileEntityRadar metaTileEntity;
    @Getter
    @Setter
    private boolean isActive;
    @Getter
    private boolean canWork;
    @Getter
    private int scanProgress = 0;



    public long getEnergyDraw() {
        if (voltageTier < 0) return 0;
        return GTValues.V[voltageTier] * (long) Math.pow(4, overclockAmount);
    }

    public boolean isDataReady() {
        return lastScan != null;
    }

    public boolean tickScan(IEnergyContainer energyContainer) {
        if (!isActive || !canWork) return false;

        int targetTicks = getScanDurationTicks();
        long draw = getEnergyDraw();

        if (energyContainer.getEnergyStored() >= draw) {
            energyContainer.changeEnergy(-draw);
            scanProgress++;

            if (scanProgress >= targetTicks) {
                if (isDataReady()) {
                    return true;
                } else {
                    scanProgress = targetTicks - 1;
                }
            }
        } else {
            isActive = false;
        }
        return false;
    }
    public void setClientProgress(int progress) {
        this.scanProgress = progress;
    }

    public MultiblockRadarLogic(MetaTileEntityRadar metaTileEntity) {
        this.voltageTier = -1;
        this.overclockAmount = 0;
        this.metaTileEntity = metaTileEntity;
    }

    public void structureFormed() {
        canWork = true;
        isActive = false;
        this.voltageTier = metaTileEntity.getTier();
        this.overclockAmount = 0;  //TODO: Decide how we want to calculate overclock based on tier
    }

    public void invalidateStructure() {
        canWork = false;
        isActive = false;
    }

    // synchronized access for reading or write to the scan results, with the returned object being a
    @NotNull
    private synchronized void storeScanResult(UUID uuid) {
        lastScan = uuid;
    }

    private boolean canScan() {
        return !metaTileEntity.getWorld().isRemote && metaTileEntity.isStructureFormed() && canWork && !isActive;
    }



    static public IntCoord2 getCoordPair(BlockPos pos) {
        return new IntCoord2(pos);
    }

    //Collect snapshot of all players and valid TEs
    public static HashMap<IntCoord2, DataPoint> collectValidEntites(World world) {
        MinecraftServer serverInstance = FMLCommonHandler.instance().getMinecraftServerInstance();
        HashMap<IntCoord2, DataPoint> entityPosMap = new HashMap<>();

        List<EntityPlayerMP> worldPlayers = serverInstance.getPlayerList().getPlayers();
        for (EntityPlayerMP player : worldPlayers) {
            entityPosMap.put(getCoordPair(player.getPosition()), new DataPoint(TargetType.PLAYER,0));
        }

        ObjectIterator<Long2IntMap.Entry> iterator = RadarDataManager.INSTANCE.getHandler(world).getMachineMap().long2IntEntrySet().iterator();
        while (iterator.hasNext()) {
            Long2IntMap.Entry entry = iterator.next();
            long packed = entry.getLongKey();
            int val = entry.getIntValue();

            int x = (int) (packed >> 32);
            int z = (int) packed;

            entityPosMap.put(new IntCoord2(x, z), new DataPoint(TargetType.STRUCTURE, val));
        }


        return entityPosMap;
    }

    public int getScanDurationTicks() {
        return switch (this.voltageTier) {
            case 4 -> 12000; // EV: 600 seconds
            case 5 -> 8000;  // IV: 400 seconds
            case 6 -> 6000;  // LuV: 300 seconds
            case 7 -> 3000;  // ZPM: 150 seconds
            case 8 -> 3000;
            default -> Integer.MAX_VALUE;
        };
    }

    public enum TargetType {
        PLAYER,
        STRUCTURE
    }

    //Note: players have no value
    public static record DataPoint(TargetType type, int value) {
    }
    public void startScan() {
        if (!canScan()) return;
        this.isActive = true;
        this.scanProgress = 0;
        this.lastScan = null;

        Map<IntCoord2, DataPoint> snapshot = collectValidEntites(metaTileEntity.getWorld());

        calculateDBSCAN(snapshot, EPS, MIN_PTS)
                .thenAccept(list -> {
                    FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
                        UUID freshId = UUID.randomUUID();
                        RadarSavedData.get().addScan(freshId, list);
                        this.storeScanResult(freshId);
                    });
                })
                .exceptionally(ex -> {
                    WFCore.LOGGER.error("DBSCAN Failed: " + ex.getMessage());
                    return null;
                });
    }

    ;
    /*
    Radar takes all loaded valid entities and players, uses clustering algorithm DBSCAN and finds
    all clusters, which in this case are bases.
    It finds a bounding box and center of the base, scans how many players are inside. This is
    to avoid possible ghost bases. (generally player and TE dense areas are bases). This  SHOULD
    be ran async and must be done before the simulated scan is done (default time: 2000 seconds),
    values such as EPS and MIN_PTS should be adjustable in GUI by player.
     */
    public static CompletableFuture<List<ClusterData>> calculateDBSCAN(Map<IntCoord2, DataPoint> objMap, int eps, int minPts) {

        return CompletableFuture.supplyAsync(() -> {
            DBSCANClusterer<IntCoord2> dbscan = new DBSCANClusterer<>(eps, minPts);

            List<Cluster<IntCoord2>> clusters = dbscan.cluster(new ArrayList<>(objMap.keySet()));

            List<ClusterData> clusterDataList = new ArrayList<>(clusters.size());

            for (Cluster<IntCoord2> cluster : clusters) {
                List<IntCoord2> clusterPoints = cluster.getPoints();

                int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
                int sumX = 0, sumZ = 0;

                for (IntCoord2 point : clusterPoints) {
                    int x = point.getX();
                    int z = point.getZ();

                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (z < minZ) minZ = z;
                    if (z > maxZ) maxZ = z;

                    sumX += x;
                    sumZ += z;
                }

                IntCoord2 boundingBoxMin = new IntCoord2(minX, minZ);
                IntCoord2 boundingBoxMax = new IntCoord2(maxX, maxZ);
                IntCoord2 clusterCenter = new IntCoord2(sumX / clusterPoints.size(), sumZ / clusterPoints.size());

                int population = 0;
                int clusterValue = 0;
                for (IntCoord2 point : clusterPoints) {
                    DataPoint dataPoint = objMap.get(point);
                    switch (dataPoint.type) {
                        case PLAYER -> population++;
                        case STRUCTURE -> {
                            if (dataPoint.value > 0)
                                clusterValue += dataPoint.value;
                            else
                                clusterValue++;
                        }
                    }
                }

                clusterDataList.add(new ClusterData(
                        clusterPoints,
                        clusterCenter,
                        new BoundingBox(boundingBoxMin, boundingBoxMax), clusterValue,
                        population));
            }
            return clusterDataList;

        });
    }


    public long getOverclockedEnergy() {
        long baseEU = GTValues.V[voltageTier];
        // Manual tuning: quadruple power per overclock
        return baseEU * (long) Math.pow(4, overclockAmount);
    }



    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        data.setBoolean("isActive", this.isActive);
        data.setInteger("scanProgress", this.scanProgress);
        if (this.lastScan != null) {
            data.setUniqueId("lastScan", this.lastScan);
        }
        return data;
    }

    public void readFromNBT(NBTTagCompound data) {
        this.isActive = data.getBoolean("isActive");
        this.scanProgress = data.getInteger("scanProgress");
        if (data.hasUniqueId("lastScan")) {
            this.lastScan = data.getUniqueId("lastScan");
        }
    }
}