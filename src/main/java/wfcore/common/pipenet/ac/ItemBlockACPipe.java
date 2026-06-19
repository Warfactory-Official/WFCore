package wfcore.common.pipenet.ac;

import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.ItemBlockPipe;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemBlockACPipe extends ItemBlockPipe<ACPipeType, ACPipeProperties> {

    public ItemBlockACPipe(BlockPipe<ACPipeType, ACPipeProperties, ?> block) {
        super(block);
    }

    @Override
    public void addInformation(@NotNull ItemStack stack, @Nullable World worldIn, @NotNull List<String> tooltip,
                               @NotNull ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        ACPipeProperties properties = blockPipe.createItemProperties(stack);
        if (properties != null) {
            tooltip.add(net.minecraft.client.resources.I18n.format("wfcore.ac_cable.throughput", properties.throughput));
        }
        tooltip.add(net.minecraft.client.resources.I18n.format("wfcore.ac_cable.desc"));
    }
}
