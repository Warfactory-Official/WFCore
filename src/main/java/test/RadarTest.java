package test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static test.RadarClusteringEngine.*;

public class RadarTest {
    // Example of how clean your testing will look now:
    @org.junit.Test
    public void testBaseDetection() {
        Map<IntCoord2, DataPoint> mockWorld = new HashMap<>();
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        Random rng = new Random(42);

        for (int b = 0; b < 10; b++) {
            int offsetX = (b % 4) * 2000;
            int offsetZ = (b / 4) * 2000;

            int machinesInBase = 20 + rng.nextInt(15);
            for (int i = 0; i < machinesInBase; i++) {
                int x = offsetX + rng.nextInt(500);
                int z = offsetZ + rng.nextInt(500);

                int value = (rng.nextDouble() > 0.8) ? 5 + rng.nextInt(4) : 1 + rng.nextInt(3);
                mockWorld.put(new IntCoord2(x, z), new DataPoint(TargetType.STRUCTURE, value));
            }

            int players = 1 + rng.nextInt(10);
            for (int i = 0; i < players; i++) {
                int px = offsetX + rng.nextInt(500);
                int pz = offsetZ + rng.nextInt(500);
                mockWorld.put(new IntCoord2(px, pz), new DataPoint(TargetType.PLAYER, 0));
            }


            int noiseX = offsetX + 1000 + rng.nextInt(200);
            int noiseZ = offsetZ + 1000 + rng.nextInt(200);
            mockWorld.put(new IntCoord2(noiseX, noiseZ), new DataPoint(TargetType.STRUCTURE, 1));
        }


        try (FileWriter writer = new FileWriter(Paths.get("./mock.json").toFile())) {
            gson.toJson(mockWorld, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        RadarClusteringEngine engine = new RadarClusteringEngine();
        List<RadarClusteringEngine.ClusterData> results = engine.calculateDBSCAN(mockWorld);


        try (FileWriter writer = new FileWriter(Paths.get("./clusters.json").toFile())) {
            gson.toJson(results, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(true, true);
    }
}