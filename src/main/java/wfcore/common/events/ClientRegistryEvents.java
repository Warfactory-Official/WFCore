package wfcore.common.events;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import wfcore.api.SelfRegisteringModel;
import wfcore.common.blocks.BlockRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class ClientRegistryEvents {

    @SideOnly(Side.CLIENT)
    private static void registerItemModel(@NotNull Block block) {
        for (IBlockState state : block.getBlockState().getValidStates()) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block),
                    block.getMetaFromState(state),
                    new ModelResourceLocation(block.getRegistryName(),
                            statePropertiesToString(state.getProperties())));
        }
    }

    public static @NotNull String statePropertiesToString(@NotNull Map<IProperty<?>, Comparable<?>> properties) {
        StringBuilder stringbuilder = new StringBuilder();

        List<Map.Entry<IProperty<?>, Comparable<?>>> entries = properties.entrySet().stream()
                .sorted(Comparator.comparing(c -> c.getKey().getName()))
                .collect(Collectors.toList());

        for (Map.Entry<IProperty<?>, Comparable<?>> entry : entries) {
            if (stringbuilder.length() != 0) {
                stringbuilder.append(",");
            }

            IProperty<?> property = entry.getKey();
            stringbuilder.append(property.getName());
            stringbuilder.append("=");
            stringbuilder.append(getPropertyName(property, entry.getValue()));
        }

        if (stringbuilder.length() == 0) {
            stringbuilder.append("normal");
        }

        return stringbuilder.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> @NotNull String getPropertyName(@NotNull IProperty<T> property, Comparable<?> value) {
        return property.getName((T) value);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onModelBake(ModelBakeEvent event) {
        SelfRegisteringModel.bakeModels(event);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPreTextureStitch(TextureStitchEvent.Pre event) {
        SelfRegisteringModel.registerSprites(event.getMap());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRegisterModels(ModelRegistryEvent event) {
        SelfRegisteringModel.registerModels();
        SelfRegisteringModel.registerCustomStateMappers();
        BlockRegistry.BLOCKS.stream().filter(block -> !(block instanceof SelfRegisteringModel)).forEach(
                ClientRegistryEvents::registerItemModel
        );


    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onItemColors(ColorHandlerEvent.Item event) {
        SelfRegisteringModel.registerItemColorHandlers(event);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onBlockColors(ColorHandlerEvent.Block event) {
        SelfRegisteringModel.registerBlockColorHandlers(event);
    }


}
