package test;

import lombok.Getter;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static wfcore.api.radar.MultiblockRadarLogic.EPS;
import static wfcore.api.radar.MultiblockRadarLogic.MIN_PTS;

public class RadarClusteringEngine {
    public List<ClusterData> calculateDBSCAN(Map<IntCoord2, DataPoint> objMap) {

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
    }


    public enum TargetType {
        PLAYER,
        STRUCTURE
    }

    //Note: players have no value
    public record DataPoint(TargetType type, int value) {
    }

    ;

    public static class IntCoord2 implements Clusterable {
        private final int x, z;

        public IntCoord2(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }


        @Override
        public double[] getPoint() {
            return new double[]{x, z};
        }

        @Override
        public String toString() {
            return "(" + x + ", " + z + ")";
        }
    }

    public class ClusterData {
        public final List<IntCoord2> coordinates;
        public final IntCoord2 centerPoint;
        public final BoundingBox boundingBox;
        public final int value;
        public final int playerPopulation;

        public ClusterData(List<IntCoord2> coordinates, IntCoord2 centerPoint, BoundingBox boundingBox, int value, int playerPopulation) {
            this.coordinates = coordinates;
            this.centerPoint = centerPoint;
            this.boundingBox = boundingBox;
            this.value = value;
            this.playerPopulation = playerPopulation;
        }

    }

    @Getter
    public class BoundingBox {
        private final IntCoord2 min, max;

        public BoundingBox(IntCoord2 min, IntCoord2 max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public String toString() {
            return "{" + min.toString() + ", " + max.toString() + "}";
        }


    }

}
