package wfcore.common.items.registry;

import gregtech.common.items.MetaItems;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class DataHolderRegistry {
    private static final LongOpenHashSet ALLOWED_KEYS = new LongOpenHashSet();

    static {
        register(MetaItems.TOOL_DATA_STICK.getStackForm());
        register(MetaItems.TOOL_DATA_ORB.getStackForm());
        //Whatever else
    }

    private static void register(ItemStack stack) {
        ALLOWED_KEYS.add(getPackedKey(stack));
    }

    public static boolean isAllowed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // This check is near-instant and creates 0 new objects
        return ALLOWED_KEYS.contains(getPackedKey(stack));
    }

    private static long getPackedKey(ItemStack stack) {
        // Pack Item ID (32 bits) and Metadata (32 bits) into one 64-bit Long
        int itemId = Item.getIdFromItem(stack.getItem());
        int meta = stack.getMetadata();
        return (((long) itemId) << 32) | (meta & 0xFFFFFFFFL);
    }
}
