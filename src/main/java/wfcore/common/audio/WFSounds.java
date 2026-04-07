package wfcore.common.audio;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import wfcore.Reference;

public class WFSounds {
    public static SoundEvent RADAR_LOOP;

    public static void register() {
        RADAR_LOOP = registerSound("radar_loop");
    }

    private static SoundEvent registerSound(String name) {
        ResourceLocation location = new ResourceLocation(Reference.MODID, name);
        SoundEvent event = new SoundEvent(location);
        event.setRegistryName(location);
        return event;
    }
}
