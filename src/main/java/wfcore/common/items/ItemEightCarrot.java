package wfcore.common.items;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wfcore.Reference;
import wfcore.api.SelfRegisteringModel;

import javax.annotation.Nullable;
import java.util.List;

public class ItemEightCarrot extends ItemFood implements SelfRegisteringModel {
    public static final String ROOT_PATH = "items/";
    String texturePath;

    public ItemEightCarrot(String s, String texturePath) {
        super(5, 0.6F, false);
        this.setRegistryName(s);
        this.setPotionEffect(new PotionEffect(MobEffects.WITHER, 5,1), 0.3f);
        this.texturePath = texturePath;
        this.setTranslationKey(s);
        INSTANCES.add(this);
        ItemRegistry.ITEMS.add(this);
    }

    public ItemEightCarrot(String s) {
        this(s,s);
    }

    @Override
    public void bakeModel(ModelBakeEvent event) {
        try {
            IModel baseModel = ModelLoaderRegistry.getModel(new ResourceLocation("minecraft", "item/generated"));
            ResourceLocation spriteLoc = new ResourceLocation(Reference.MODID, ROOT_PATH + texturePath);
            IModel retexturedModel = baseModel.retexture(
                    ImmutableMap.of(
                            "layer0", spriteLoc.toString()
                    )

            );
            IBakedModel bakedModel = retexturedModel.bake(ModelRotation.X0_Y0, DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
            ModelResourceLocation bakedModelLocation;
            bakedModelLocation = new ModelResourceLocation(spriteLoc, "inventory");
            event.getModelRegistry().putObject(bakedModelLocation, bakedModel);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void registerModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(new ResourceLocation(Reference.MODID, ROOT_PATH + texturePath), "inventory"));
    }

    @Override
    public void registerSprite(TextureMap map) {
        map.registerSprite(new ResourceLocation(Reference.MODID, ROOT_PATH + texturePath));
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add("Burnt on the outside, raw on the inside");
        tooltip.add("Eight's favourite");
    }



}
