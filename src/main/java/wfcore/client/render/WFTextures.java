package wfcore.client.render;

import gregtech.client.renderer.texture.cube.OrientedOverlayRenderer;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WFTextures {

    public static OrientedOverlayRenderer OVERLAY_RADAR;
    public static OrientedOverlayRenderer OVERLAY_COMPUTER;
    public static SimpleOverlayRenderer ALU_SHEET;

    @SideOnly(Side.CLIENT)
    public static void registerTextures(){
        OVERLAY_RADAR = new OrientedOverlayRenderer("multiblock/radar");
        OVERLAY_COMPUTER = new OrientedOverlayRenderer("multiblock/computer");
        ALU_SHEET = new SimpleOverlayRenderer( "casings/wfcore_sheet_casing/aluminium_sheet_casing");
    }

}
