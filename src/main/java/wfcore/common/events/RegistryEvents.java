package wfcore.common.events;

import gregtech.api.block.VariantBlock;
import gregtech.api.unification.material.event.MaterialEvent;
import gregtech.api.unification.material.event.PostMaterialEvent;
import gregtech.common.items.MetaItems;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.NotNull;
import wfcore.api.block.ICustomBlockItem;
import wfcore.api.material.modifications.WFCoreMaterialExtraFlags;
import wfcore.api.material.ore.WFCoreOrePrefix;
import wfcore.api.material.ore.WFCoreRecipeHandler;
import wfcore.common.blocks.BlockRegistry;
import wfcore.common.items.ItemRegistry;
import wfcore.common.materials.WFMaterials;
import wfcore.common.metatileentities.WFCoreMetaTileEntities;
import wfcore.common.recipe.GregtechRecipes;
import wfcore.common.recipe.VanillaRecipes;
import wfcore.common.recipe.chain.LargeBlastFurnace;
import wfcore.common.recipe.chain.SteamWiremillRecipes;

import java.util.function.Function;

import static wfcore.common.items.ItemRegistry.createVariantItemBlockUnchecked;

public class RegistryEvents {


    @SubscribeEvent(priority = EventPriority.HIGH)
    public void registerMaterials(@NotNull MaterialEvent event) {
        WFMaterials.register();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void postRegisterMaterials(@NotNull PostMaterialEvent event) {
    }

    @SubscribeEvent
    public void registerBlocks(@NotNull RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        BlockRegistry.BLOCKS.forEach(registry::register);
        WFCoreMetaTileEntities.init();
    }

    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        WFCoreRecipeHandler.init();
        SteamWiremillRecipes.init();
        LargeBlastFurnace.init();
        VanillaRecipes.registerCTRecipes(event);
        VanillaRecipes.registerFurnaceRecipes(event);
        GregtechRecipes.registerGregTechRecipes();    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        ItemRegistry.ITEMS.forEach(registry::register);
        for (Block block : BlockRegistry.BLOCKS) {
            Function<Block, ItemBlock> producer = _ ->
                    block instanceof VariantBlock<?>
                            ? createVariantItemBlockUnchecked((VariantBlock<?>) block)
                            : new ItemBlock(block);
            if (block instanceof ICustomBlockItem custom)
                producer = custom.getItemBlock();
            ItemRegistry.createItemBlock(block, producer).ifPresent(registry::register);
        }

    }

    @SubscribeEvent
    public void materialChanges(PostMaterialEvent event) {
        MetaItems.addOrePrefix(WFCoreOrePrefix.billet);
        MetaItems.addOrePrefix(WFCoreOrePrefix.ntmpipe);
        MetaItems.addOrePrefix(WFCoreOrePrefix.wiredense);
        MetaItems.addOrePrefix(WFCoreOrePrefix.shell);
        MetaItems.addOrePrefix(WFCoreOrePrefix.plateTriple);
        MetaItems.addOrePrefix(WFCoreOrePrefix.plateSextuple);
        WFCoreMaterialExtraFlags.register();
    }
}
