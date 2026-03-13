package wfcore.common.te;

import com.modularmods.mcgltf.MCglTF;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wfcore.Tags;
import wfcore.api.metatileentity.IAnimatedMTE;
import wfcore.api.metatileentity.MteRenderer;
import wfcore.common.metatileentities.multi.electric.MetaTileEntityRadar;
import wfcore.common.render.AnimatablePartRenderer;
import wfcore.common.render.GenericGLTF;

import java.util.HashMap;
import java.util.Map;

public class TERegistry {

    @SideOnly(Side.CLIENT)
    private static Map<Class<? extends MetaTileEntity>, MteRenderer<? extends MetaTileEntity>> mteRenderMap = new HashMap<>();

    @SideOnly(Side.CLIENT)
    public static void registerRenderers() {
        ClientRegistry.bindTileEntitySpecialRenderer(AnimatablePartTileEntity.class, new AnimatablePartRenderer());
        registerRenderer(MetaTileEntityRadar.class, new GenericGLTF<>(
                new ResourceLocation(Tags.MODID, "model/radar.gltf")
        ));


    }

    @SideOnly(Side.CLIENT)
    public static <T extends MetaTileEntity & IAnimatedMTE> void registerRenderer(
            Class<T> mteClass,
            MteRenderer<T> renderer
    ) {
        mteRenderMap.put(mteClass, renderer);
        MCglTF.getInstance().addGltfModelReceiver(renderer);
    }

    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public static <T extends MetaTileEntity & IAnimatedMTE>
    MteRenderer<T> getRenderer(Class<? extends MetaTileEntity> clazz) {
        Class<?> c = clazz;

        while (c != null && MetaTileEntity.class.isAssignableFrom(c)) {
            MteRenderer<?> renderer = mteRenderMap.get(c);
            if (renderer != null) {
                return (MteRenderer<T>) renderer;
            }
            c = c.getSuperclass();
        }

        return null;
    }
}
