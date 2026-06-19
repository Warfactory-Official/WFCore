package wfcore.common.pipenet.ac;

import wfcore.common.blocks.BlockRegistry;

/** Creates and holds the AC cable blocks (one per thickness). Steel wire = {@link ACPipeType#SINGLE}. */
public final class ACPipes {

    public static final BlockACPipe[] AC_CABLES = new BlockACPipe[ACPipeType.VALUES.length];

    private static boolean initialized = false;

    private ACPipes() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        for (ACPipeType type : ACPipeType.VALUES) {
            BlockACPipe block = new BlockACPipe(type);
            block.setRegistryName("ac_cable_" + type.name);
            block.setTranslationKey("ac_cable_" + type.name);
            AC_CABLES[type.ordinal()] = block;
            BlockRegistry.BLOCKS.add(block);
        }
    }
}
