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

        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().entityRenderer.enableLightmap();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.colorMaterial(1032, 5634);
        org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
        org.lwjgl.opengl.GL11.glPushClientAttrib(-1);

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

        org.lwjgl.opengl.GL11.glPopClientAttrib();
        org.lwjgl.opengl.GL11.glPopAttrib();
        Minecraft.getMinecraft().entityRenderer.disableLightmap();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableColorMaterial();

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(true);

        GlStateManager.popMatrix();


        activeCount = 0;
    }
}
