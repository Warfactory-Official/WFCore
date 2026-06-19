package wfcore.client.render;

import codechicken.lib.vec.uv.IconTransformation;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.Material;
import gregtech.client.renderer.pipe.PipeRenderer;
import gregtech.client.renderer.texture.Textures;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import wfcore.Reference;
import wfcore.common.pipenet.ac.ACPipeType;

/**
 * Renderer for AC cables, reusing GregTech's generic pipe renderer base. Uses the laser-pipe sprites as a
 * placeholder texture (a dedicated steel-wire texture can replace these later).
 */
public class ACPipeRenderer extends PipeRenderer {

    public static final ACPipeRenderer INSTANCE = new ACPipeRenderer();

    private TextureAtlasSprite wireTexture;

    public ACPipeRenderer() {
        super("wfcore_ac_cable", new ResourceLocation(Reference.MODID, "ac_cable"));
    }

    @Override
    public void registerIcons(TextureMap map) {
        this.wireTexture = Textures.LASER_PIPE_SIDE;
    }

    @Override
    public void buildRenderer(PipeRenderContext renderContext, BlockPipe<?, ?, ?> blockPipe,
                              @Nullable IPipeTile<?, ?> pipeTile, IPipeType<?> pipeType, @Nullable Material material) {
        if (pipeType instanceof ACPipeType) {
            renderContext.addOpenFaceRender(new IconTransformation(Textures.LASER_PIPE_IN))
                    .addSideRender(false, new IconTransformation(getWireTexture()));
        }
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return getWireTexture();
    }

    private TextureAtlasSprite getWireTexture() {
        return wireTexture == null ? Textures.LASER_PIPE_SIDE : wireTexture;
    }
}
