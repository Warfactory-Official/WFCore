package wfcore.client.render;

import com.modularmods.mcgltf.RenderedGltfModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
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

        //Note: this is fully safe, since I basically clear the state
        //Sure I am the USE GLSTATEMANAGER guy, but this should be fine, trust me
        Minecraft.getMinecraft().entityRenderer.enableLightmap();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

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
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL11.glPopAttrib();



        Minecraft.getMinecraft().entityRenderer.disableLightmap();

        activeCount = 0;
    }
}
