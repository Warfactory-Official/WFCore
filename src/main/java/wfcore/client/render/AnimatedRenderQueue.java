package wfcore.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import wfcore.api.metatileentity.IAnimatedMTE;
import wfcore.common.te.TERegistry;

import java.util.Arrays;

@SideOnly(Side.CLIENT)
public class AnimatedRenderQueue {

    private static final AnimatedRenderQueue INSTANCE = new AnimatedRenderQueue();
    private IAnimatedMTE[] pool;
    private int activeCount = 0;
    private int capacity;

    private AnimatedRenderQueue() {
        this.capacity = 512;
        this.pool = new IAnimatedMTE[capacity];
    }

    public static AnimatedRenderQueue getInstance() {
        return INSTANCE;
    }


    public void submit(IAnimatedMTE mte) {
        if (activeCount >= capacity) {
            expand(20);
        }
        pool[activeCount++] = mte;
    }

    private void expand(int extra) {
        this.capacity += extra;
        this.pool = Arrays.copyOf(pool, capacity);
    }


    public void flush(float partialTicks) {
        if (activeCount == 0) return;

        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        double camX = rm.viewerPosX;
        double camY = rm.viewerPosY;
        double camZ = rm.viewerPosZ;

        Minecraft.getMinecraft().entityRenderer.enableLightmap();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();

        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);

        for (int i = 0; i < activeCount; i++) {
            IAnimatedMTE mte = pool[i];
            if (mte != null) {
                var pos = mte.thisObject().getPos();
                double x = (double) pos.getX() - camX;
                double y = (double) pos.getY() - camY;
                double z = (double) pos.getZ() - camZ;

                TERegistry.getRenderer(mte.thisObject().getClass())
                        .render(mte.thisObject(), x, y, z, partialTicks);

                pool[i] = null;
            }
        }


        //Hopefully this will prevent minecraft from shitting itself
        net.minecraft.client.renderer.OpenGlHelper.glUseProgram(0);

        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_FLAT);

        GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();

        //Unbind VBOs (Stops particles from reading GLTF memory)
        net.minecraft.client.renderer.OpenGlHelper.glBindBuffer(
                net.minecraft.client.renderer.OpenGlHelper.GL_ARRAY_BUFFER, 0);

        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableRescaleNormal();
        Minecraft.getMinecraft().entityRenderer.disableLightmap();

        activeCount = 0;
    }
}
