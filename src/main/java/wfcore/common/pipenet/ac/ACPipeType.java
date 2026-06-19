package wfcore.common.pipenet.ac;

import gregtech.api.pipenet.block.IPipeType;

/**
 * AC cable thickness tiers. Throughput scales with the number of strands but with diminishing returns
 * (sqrt of strand count), so a single steel wire carries the base 512 EU/t and thicker cables add less per
 * strand.
 */
public enum ACPipeType implements IPipeType<ACPipeProperties> {

    SINGLE("single", 0.25f, 1),
    DOUBLE("double", 0.375f, 2),
    QUADRUPLE("quadruple", 0.5f, 4),
    OCTAL("octal", 0.75f, 8),
    HEX("hex", 1.0f, 16);

    public static final ACPipeType[] VALUES = values();

    public final String name;
    public final float thickness;
    public final int strands;

    ACPipeType(String name, float thickness, int strands) {
        this.name = name;
        this.thickness = thickness;
        this.strands = strands;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getThickness() {
        return thickness;
    }

    @Override
    public ACPipeProperties modifyProperties(ACPipeProperties baseProperties) {
        return new ACPipeProperties((long) (baseProperties.throughput * Math.sqrt(strands)));
    }

    @Override
    public boolean isPaintable() {
        return true;
    }
}
