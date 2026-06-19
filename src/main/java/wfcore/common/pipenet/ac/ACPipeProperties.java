package wfcore.common.pipenet.ac;

/** Node data for AC cables: a flat EU/t throughput. */
public class ACPipeProperties {

    public static final ACPipeProperties EMPTY = new ACPipeProperties(0);

    public final long throughput;

    public ACPipeProperties(long throughput) {
        this.throughput = throughput;
    }
}
