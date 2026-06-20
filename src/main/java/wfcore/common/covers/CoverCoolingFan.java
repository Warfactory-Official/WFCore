package wfcore.common.covers;

import gregtech.api.cover.CoverBase;
import gregtech.api.cover.CoverDefinition;
import gregtech.api.cover.CoverableView;

import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import org.jetbrains.annotations.NotNull;
import wfcore.client.render.WFTextures;

/**
 * A tiered cooling-fan cover. Placed on a mainframe Cooling Fan's exposed face, it boosts that hatch's passive
 * cooling proportionally to its tier (one definition per voltage tier, LV..EV). The cooling part reads
 * {@link #getTier()} via {@code getCoverAtSide}.
 */
public class CoverCoolingFan extends CoverBase {

    private final int tier;

    public CoverCoolingFan(@NotNull CoverDefinition definition, @NotNull CoverableView coverableView,
                           @NotNull EnumFacing attachedSide, int tier) {
        super(definition, coverableView, attachedSide);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public boolean canAttach(@NotNull CoverableView coverable, @NotNull EnumFacing side) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderCover(@NotNull CCRenderState renderState, @NotNull Matrix4 translation,
                            IVertexOperation[] pipeline, @NotNull Cuboid6 plateBox, @NotNull BlockRenderLayer layer) {
        WFTextures.COOLING_FAN_COVER.renderSided(getAttachedSide(), plateBox, renderState, pipeline, translation);
    }
}
