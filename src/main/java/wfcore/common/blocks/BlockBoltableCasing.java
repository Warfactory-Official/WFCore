package wfcore.common.blocks;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.ImmutableList;
import com.hbm.api.block.IToolable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.util.I18nUtil;
import gregtech.api.block.IStateHarvestLevel;
import gregtech.api.block.VariantBlock;
import gregtech.api.items.toolitem.ToolClasses;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.ore.OrePrefix;
import lombok.Getter;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.api.block.IToolableVariant;
import wfcore.api.items.AbstractStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

//TODO: Abstract this out
public class BlockBoltableCasing extends VariantBlock<BlockBoltableCasing.BoltableCasingType> implements IToolable, ILookOverlay {

    public static final BiMap<BoltableCasingType, BoltableCasingType> conversionMap = EnumHashBiMap.create(BoltableCasingType.class);
    protected static volatile ImmutableList<AbstractStack> cachedCost;

    static {
        conversionMap.put(BoltableCasingType.BORON_COATED, BoltableCasingType.BORON_COATED_BOLTED);
    }


    public BlockBoltableCasing(String name) {
        super(Material.IRON);
        setTranslationKey(name);
        setRegistryName(name);
        setHardness(5.0f);
        setResistance(10.0f);
        setSoundType(SoundType.METAL);
        BlockRegistry.BLOCKS.add(this);

    }


    protected ItemStack getSilkTouchDrop(IBlockState state)
    {
        return new ItemStack(state.getBlock(), 1, damageDropped(state));
    }

    @Override
    public int damageDropped(@NotNull IBlockState state) {
        var theEnum = state.getValue(VARIANT);
        var inverseEnum = conversionMap.inverse().get(theEnum);
        if (inverseEnum != null) {
            return getMetaFromState(state.withProperty(VARIANT, inverseEnum));
        }
        return getMetaFromState(state);
    }

    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        super.getDrops(drops, world, pos, state, fortune);
        var theEnum = state.getValue(VARIANT);
        var inverseEnum = conversionMap.inverse().get(theEnum);
        if (inverseEnum != null) {
            inverseEnum.getCost().stream().map(AbstractStack::getStack).forEach(drops::add);
        }

    }


    @Override
    public boolean canCreatureSpawn(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos,
                                    @NotNull EntityLiving.SpawnPlacementType type) {
        return false;
    }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float fX, float fY, float fZ, EnumHand hand, ToolType tool) {
        if (world.isRemote) return false;
        BlockPos pos = new BlockPos(x, y, z);
        var state = world.getBlockState(pos);
        BoltableCasingType stateEnum = state.getValue(VARIANT);
        if (stateEnum.getTool() == null) return false;
        BoltableCasingType convertedEnum = conversionMap.get(stateEnum);
        if (convertedEnum == null) return false;
        if (stateEnum.getCost().isEmpty() || AbstractStack.resolveCost(player, stateEnum.getCost(), !player.isCreative())) {
            world.setBlockState(pos, state.withProperty(VARIANT, convertedEnum));
            return true;
        }
        return false;
    }

    @Override
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        ItemStack held = Minecraft.getMinecraft().player.getHeldItemMainhand();
        ToolType tool = ToolType.getType(held);
        if (tool == null) return;

        var state = world.getBlockState(pos);
        BoltableCasingType stateEnum = state.getValue(VARIANT);
        BoltableCasingType convertedEnum = conversionMap.get(stateEnum);
        if (convertedEnum == null) return;


        List<String> text = new ArrayList<>();
        text.add(TextFormatting.GOLD + "Requires:");

        for (AbstractStack stack : stateEnum.getCost()) {
            try {
                ItemStack display = stack.extractForCyclingDisplay(20);
                text.add("- " + display.getDisplayName() + " x" + display.getCount());
            } catch (Exception ex) {
                text.add(TextFormatting.RED + "- ERROR");
            }
        }

        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(state.getBlock().getTranslationKey()+".name"), 0xffff00, 0x404000, text);

    }

    @SideOnly(Side.CLIENT)
    public void printHook(RenderGameOverlayEvent.Pre event, World world, int x, int y, int z) {
        printHook(event, world, new BlockPos(x, y, z));
    }


    public enum BoltableCasingType
            implements IStringSerializable, IStateHarvestLevel, IToolableVariant {

        BORON_COATED(
                "boron_coated",
                2,
                ToolType.BOLT,
                () -> new AbstractStack(
                        OreDictUnifier.get(OrePrefix.bolt, Materials.StainlessSteel, 8)
                )
        ),

        BORON_COATED_BOLTED("boron_coated_bolted", 2);

        @Getter
        private final String name;

        private final int harvestLevel;

        @Nullable
        private final IToolable.ToolType tool;

        private final ImmutableList<Supplier<AbstractStack>> costSuppliers;


        @SafeVarargs
        BoltableCasingType(
                String name,
                int harvestLevel,
                @Nullable IToolable.ToolType tool,
                Supplier<AbstractStack>... cost
        ) {
            this.name = name;
            this.harvestLevel = harvestLevel;
            this.tool = tool;
            this.costSuppliers = ImmutableList.copyOf(cost);
        }

        BoltableCasingType(
                String name,
                int harvestLevel,
                Supplier<AbstractStack>... cost
        ) {
            this(name, harvestLevel, null, cost);
        }

        BoltableCasingType(String name, int harvestLevel) {
            this.name = name;
            this.harvestLevel = harvestLevel;
            this.tool = null;
            this.costSuppliers = ImmutableList.of();
        }


        @Override
        public int getHarvestLevel(IBlockState state) {
            return harvestLevel;
        }

        @Override
        public String getHarvestTool(IBlockState state) {
            return ToolClasses.WRENCH;
        }

        @Override
        public @Nullable IToolable.ToolType getTool() {
            return tool;
        }


        @SuppressWarnings("all")
        @Override
        public @NotNull ImmutableList<AbstractStack> getCost() {//Had to make it lazy to prevent the game from loading air
            ImmutableList<AbstractStack> local = cachedCost;
            if (local == null) {
                synchronized (this) {
                    local = cachedCost;
                    if (local == null) {
                        local = costSuppliers.stream()
                                .map(Supplier::get)
                                .collect(ImmutableList.toImmutableList());
                        cachedCost = local;
                    }
                }
            }
            return local;
        }


    }

}

