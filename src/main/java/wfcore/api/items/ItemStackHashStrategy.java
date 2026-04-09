package wfcore.api.items;

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.item.ItemStack;

public class ItemStackHashStrategy implements Hash.Strategy<ItemStack> {

    @Override
    public int hashCode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int result = stack.getItem().hashCode();
        result = 31 * result + stack.getMetadata();
        if (stack.hasTagCompound()) {
            result = 31 * result + stack.getTagCompound().hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(ItemStack s1, ItemStack s2) {
        if (s1 == s2) return true;
        if (s1 == null || s2 == null) return false;
        return s1.isItemEqual(s2) && ItemStack.areItemStackTagsEqual(s1, s2);
    }
}
