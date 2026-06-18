package wfcore.api.radar;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import wfcore.api.util.math.ClusterData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a written book from a completed radar scan. Bases are ranked by richness, then by player
 * count found inside the scan box; each entry reports the estimated center coordinates.
 */
public final class RadarBookFactory {

    private static final int ENTRIES_PER_PAGE = 3;

    private RadarBookFactory() {}

    public static ItemStack createReport(List<ClusterData> clusters) {
        List<ClusterData> ranked = new ArrayList<>(clusters);
        ranked.sort(Comparator
                .comparingInt((ClusterData c) -> c.clusterValue)
                .thenComparingInt(c -> c.playerPopulation)
                .reversed());

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "Radar Scan Report");
        tag.setString("author", "WFCore Radar");

        NBTTagList pages = new NBTTagList();
        pages.appendTag(new NBTTagString(page("§l§9Radar Scan Report§r\n\n"
                + ranked.size() + " base(s) detected.\n\n"
                + "Ranked by richness and player presence inside the scan box.")));

        StringBuilder current = new StringBuilder();
        int rank = 1;
        int onPage = 0;
        for (ClusterData cluster : ranked) {
            current.append("§l#").append(rank).append(" Base§r\n")
                    .append("Center: ").append(cluster.centerPoint.getX()).append(", ")
                    .append(cluster.centerPoint.getZ()).append('\n')
                    .append("Richness: ").append(cluster.clusterValue).append('\n')
                    .append("Players: ").append(cluster.playerPopulation).append("\n\n");
            rank++;
            if (++onPage >= ENTRIES_PER_PAGE) {
                pages.appendTag(new NBTTagString(page(current.toString())));
                current.setLength(0);
                onPage = 0;
            }
        }
        if (current.length() > 0) {
            pages.appendTag(new NBTTagString(page(current.toString())));
        }

        tag.setTag("pages", pages);
        book.setTagCompound(tag);
        return book;
    }

    private static String page(String text) {
        return ITextComponent.Serializer.componentToJson(new TextComponentString(text));
    }
}
