package wfcore.common.covers;

import gregtech.api.GTValues;
import gregtech.api.GregTechAPI;
import gregtech.api.cover.CoverDefinition;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import wfcore.Reference;

import java.util.Locale;

/**
 * Registers the tiered cooling-fan covers (one definition per tier, LV..EV) and their placer items.
 *
 * <p>{@link #createItems()} runs during item registration (the placer items self-add to the item registry);
 * {@link #registerDefinitions()} runs afterwards, once the items exist, to register the cover definitions and
 * wire each placer item to its definition. Network ids start at {@link #ID_BASE} to avoid colliding with
 * GregTech's own covers.
 */
public final class WFCoreCovers {

    public static final int[] FAN_TIERS = { GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV };
    private static final int ID_BASE = 31000;

    public static final ItemCoverPlacer[] FAN_COVER_ITEMS = new ItemCoverPlacer[GTValues.EV + 1];
    public static final CoverDefinition[] FAN_COVERS = new CoverDefinition[GTValues.EV + 1];

    private WFCoreCovers() {}

    public static void createItems() {
        if (FAN_COVER_ITEMS[GTValues.LV] != null) return;
        for (int tier : FAN_TIERS) {
            String vn = GTValues.VN[tier].toLowerCase(Locale.ROOT);
            FAN_COVER_ITEMS[tier] = new ItemCoverPlacer("cooling_fan_cover_" + vn, "cooling_fan_cover");
        }
    }

    public static void registerDefinitions() {
        createItems();
        if (FAN_COVERS[GTValues.LV] != null) return;
        for (int tier : FAN_TIERS) {
            final int t = tier;
            ResourceLocation id = new ResourceLocation(Reference.MODID,
                    "cooling_fan." + GTValues.VN[t].toLowerCase(Locale.ROOT));
            CoverDefinition def = new CoverDefinition(id,
                    (definition, view, side) -> new CoverCoolingFan(definition, view, side, t),
                    new ItemStack(FAN_COVER_ITEMS[t]));
            GregTechAPI.COVER_REGISTRY.register(ID_BASE + t, id, def);
            FAN_COVERS[t] = def;
            FAN_COVER_ITEMS[t].setDefinition(def);
        }
    }
}
