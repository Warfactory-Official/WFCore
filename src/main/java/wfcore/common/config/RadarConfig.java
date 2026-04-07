package wfcore.common.config;

import gregtech.api.metatileentity.MetaTileEntity;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.tileentity.TileEntity;
import org.yaml.snakeyaml.Yaml;
import wfcore.WFCore;
import wfcore.api.radar.RadarTargetIdentifier;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import static wfcore.api.util.LogUtil.logExceptionWithTrace;

public class RadarConfig {

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
            whitelist.add(new RadarTargetIdentifier(regName, stateStr).intensity(value));
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


    public static boolean isOnTEWhitelist(TileEntity tileEntity) {
        RadarTargetIdentifier currId = RadarTargetIdentifier.getBestIdentifier(tileEntity);
        return TE_WHITELIST.contains(currId);
    }

    public static boolean isOnTEWhitelist(MetaTileEntity tileEntity) {
        RadarTargetIdentifier currId = new RadarTargetIdentifier(tileEntity.metaTileEntityId.toString(), null);
        return TE_WHITELIST.contains(currId);
    }

    public static int getValue(TileEntity tileEntity) {
        RadarTargetIdentifier currId = RadarTargetIdentifier.getBestIdentifier(tileEntity);
        return TE_WHITELIST.get(currId).intensity;
    }

    public static int getValue(MetaTileEntity tileEntity) {
        RadarTargetIdentifier currId = new RadarTargetIdentifier(tileEntity.metaTileEntityId.toString(), null);
        return TE_WHITELIST.get(currId).intensity;
    }

    public static int getValue(RadarTargetIdentifier target) {
        return TE_WHITELIST.get(target).intensity;
    }
}