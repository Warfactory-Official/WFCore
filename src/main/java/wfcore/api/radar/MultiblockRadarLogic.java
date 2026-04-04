package wfcore.api.radar;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import wfcore.WFCore;
import wfcore.api.util.math.ClusterData;
import wfcore.api.util.math.IntCoord2;
import wfcore.api.util.math.BoundingBox;
import wfcore.common.managers.RadarDataManager;
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
    public List<ClusterData> lastScan = null;
    private final MetaTileEntityRadar metaTileEntity;
    private boolean isActive;
    private boolean canWork;

    public static ObjectOpenHashSet<RadarTargetIdentifier> TE_WHITELIST = new ObjectOpenHashSet<>();
    public static ObjectOpenHashSet<RadarTargetIdentifier> ENTITY_WHITELIST = new ObjectOpenHashSet<>();

    private static final Path CONFIG_PATH = Paths.get("config/" + WFCore.MODID + "/radar.cfg");

    private static final List<String> EXAMPLE_YAML = List.of(
            "# Controls basic radar functionality",
            "TileEntity:",
            " - RegName: gregtech:cutter.uv # Example with GT MetaID",
            "   value: 10",
            " - RegName: minecraft:furnace # Vanilla Example",
            "   value: 1",
            " - RegName: hbm:tileentity_charge # TE+Block Example",
            "   State: hbm:charge_semtex",
            "   value: 2"
    );

    // this may be useful eventually, but currently handling each mod's method of registering tile entities is too involved
    public static void readRadarConfig() {
        Map<String, Object> globalRadarData;
        WFCore.LOGGER.atDebug().log("Beginning reading of radar config.");

        // read from the file
        try {
            writeStubIfEmpty();
            InputStream inputStream = new FileInputStream(CONFIG_PATH.toFile());  // will throw if no file exists
            globalRadarData = new Yaml().load(inputStream);  // get a mapping of keys to objects
        } catch (IOException ioError) {
            logExceptionWithTrace("Failed in stub write and config parse step [" + ioError.getMessage() + "]. Trace:", ioError);
            return;
        } catch (Exception genericError) {
            logExceptionWithTrace("Failed with unknown error in stub write and config parse step", genericError);
            return;
        }

        // interpret the file
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) globalRadarData.get("TileEntity");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) globalRadarData.get("entities");

        populateWhitelist(TE_WHITELIST, blocks);
        populateWhitelist(ENTITY_WHITELIST, entities);

        WFCore.LOGGER.atDebug().log("Finished reading radar config");
    }



    private static void populateWhitelist(
            ObjectOpenHashSet<RadarTargetIdentifier> whitelist,
            List<Map<String, Object>> targetContainer) {
        if (targetContainer == null) return;
        for (Map<String, Object> entry : targetContainer) {
            String regName = (String) entry.get("RegName");
            int value = ((Number) entry.getOrDefault("value", 1)).intValue();
            String stateStr = (String) entry.get("State");
            whitelist.add(new RadarTargetIdentifier(regName,stateStr).intensity(value));
        }
    }

    public static void writeStubIfEmpty() throws IOException {
        if (Files.notExists(CONFIG_PATH) || Files.size(CONFIG_PATH) == 0) {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.write(
                    CONFIG_PATH,
                    EXAMPLE_YAML,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        }
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
    private synchronized void storeScanResult(List<ClusterData> input) {
        lastScan = input;
    }

    private boolean canScan() {
        return !metaTileEntity.getWorld().isRemote && metaTileEntity.isStructureFormed() && canWork() && !isActive();
    }

        /* Scan should do as follows:
        1. Make sure that its on server
        2. make sure it can scan and is enabled, it should not activate on power on, player must trigger scan
             in GUI by hand
        3. make sure dataslot (custom TE that can only hold data item such as memory stick or orb) has empty valid data item (should work with data bank)
        4. Grab snapshot of valid TEs and players (this ofc is done on server thread)
        5. Run calculation
        6. Put it on data stick so it can then be put into a printer (basically port of NH printer), for data to be put in a book
         */
    //Perhaps some visualization in the tablet?
    //OPTIONAL: integrate Map mod with the mod, so players have bounding boxes drawn on their minimap, with all valid TEs  pointed out and waypoints to the centers

    public boolean performScan() {
        // only scan on server
        if (!canScan()) { return false; }

        //Scanner cannot perform a scan if data is already written
        if(true || !dataSlotIsEmpty() && !dataSlotIsWritten()){
            //Get the snapshot of all loaded players TEs
            Map<IntCoord2,DataPoint> loadedValidObjects = this.collectValidEntites();
            //Run dbscan
            //this.scanResults = clusterData;
            calculateDBSCAN(loadedValidObjects).thenAccept(this::storeScanResult).exceptionally(ex -> {
                System.err.println("Error during DBSCAN calculation: " + ex.getMessage());
                return null;
            });
        }

        return true;
    }

    public static boolean isOnTEWhitelist(TileEntity tileEntity) {
        RadarTargetIdentifier currId = RadarTargetIdentifier.getBestIdentifier(tileEntity);
        return TE_WHITELIST.contains(currId);
    }

    public static int getValue(TileEntity tileEntity) {
        RadarTargetIdentifier currId = RadarTargetIdentifier.getBestIdentifier(tileEntity);
        return TE_WHITELIST.get(currId).intensity;
    }
    public static int getValue(RadarTargetIdentifier target) {
        return TE_WHITELIST.get(target).intensity;
    }

    static public IntCoord2 getCoordPair(BlockPos pos) {
        return new IntCoord2(pos);
    }

    //Collect snapshot of all players and valid TEs
    private HashMap<IntCoord2, DataPoint> collectValidEntites() {
        MinecraftServer serverInstance = FMLCommonHandler.instance().getMinecraftServerInstance();
        HashMap<IntCoord2, DataPoint> entityPosMap = new HashMap<>();

        List<EntityPlayerMP> worldPlayers = serverInstance.getPlayerList().getPlayers();
        for (EntityPlayerMP player : worldPlayers) {
            entityPosMap.put(getCoordPair(player.getPosition()), new DataPoint(TargetType.PLAYER,0));
        }

        ObjectIterator<Long2IntMap.Entry> iterator = RadarDataManager.INSTANCE.getHandler(metaTileEntity.getWorld()).getMachineMap().long2IntEntrySet().iterator();
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

    public enum TargetType {
        PLAYER,
        STRUCTURE
    }

    //Note: players have no value
    public record DataPoint(TargetType type, int value) {
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
    private CompletableFuture<List<ClusterData>> calculateDBSCAN(Map<IntCoord2, DataPoint> objMap) {

        return CompletableFuture.supplyAsync(() -> {
            DBSCANClusterer<IntCoord2> dbscan = new DBSCANClusterer<>(EPS, MIN_PTS);

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


    public int getVoltageTier() {
        return voltageTier;
    }

    public int getOverclockAmount() {
        return this.overclockAmount;
    }

    public boolean canWork() {
        return canWork;
    }

    public boolean isActive() {
        return isActive;
    }


    public boolean dataSlotIsEmpty(){
        //FIXME
        return false;
    }

    public boolean dataSlotIsWritten(){
        //FIXME
        return false;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        if (lastScan == null) { return data; }
        if (lastScan.isEmpty()) { return data; }

        NBTTagList lastScan = new NBTTagList();
        this.lastScan.forEach(scanResult -> lastScan.appendTag(scanResult.toNBT()));
        data.setTag("last_scan", lastScan);
        return data;
    }

    public void readFromNBT(NBTTagCompound data) {
        // don't overwrite existing data
        if (lastScan != null && lastScan.size() > 0) { return; }
        if (!data.hasKey("last_scan")) { return; }

        // read the last scan
        NBTTagList lastScan = data.getTagList("last_scan", 10);
        this.lastScan = new ArrayList<>();
        lastScan.tagList.forEach(tag -> this.lastScan.add(ClusterData.fromNBT((NBTTagCompound) tag)));
    }
}