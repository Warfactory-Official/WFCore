package wfcore.client.render;

import gregtech.client.renderer.texture.cube.OrientedOverlayRenderer;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WFTextures {

    public static OrientedOverlayRenderer OVERLAY_RADAR;
    public static OrientedOverlayRenderer OVERLAY_COMPUTER;
    public static SimpleOverlayRenderer OVERLAY_CPU_SLOT;
    public static SimpleOverlayRenderer OVERLAY_CPU_SLOT_FILLED;
    public static SimpleOverlayRenderer ALU_SHEET;
    public static SimpleOverlayRenderer COOLING_FAN_COVER;

    @SideOnly(Side.CLIENT)
    public static void registerTextures(){
        OVERLAY_RADAR = new OrientedOverlayRenderer("multiblock/radar");
        OVERLAY_COMPUTER = new OrientedOverlayRenderer("multiblock/computer");
        OVERLAY_CPU_SLOT = new SimpleOverlayRenderer("overlay/overlay_cpu_slot");
        OVERLAY_CPU_SLOT_FILLED = new SimpleOverlayRenderer("overlay/overlay_cpu_slot_filled");
        ALU_SHEET = new SimpleOverlayRenderer( "casings/wfcore_sheet_casing/aluminium_sheet_casing");
        COOLING_FAN_COVER = new SimpleOverlayRenderer("cover/overlay_cooling_fan");
    }

}
