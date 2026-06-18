package wfcore.api.research;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Per-network research progress (the research database). Tracks, for each research, how many runs are
 * complete and the partial compute banked into the in-progress run. Persisted in the controller's NBT so a
 * research resumes exactly where it left off when the player switches researches mid-progress.
 */
public final class ResearchState {

    private static final String KEY_RUNS = "completedRuns";
    private static final String KEY_PARTIAL = "partialCWU";

    private final Object2IntMap<String> completedRuns = new Object2IntOpenHashMap<>();
    private final Object2LongMap<String> partialCWU = new Object2LongOpenHashMap<>();

    public int getCompletedRuns(String researchId) {
        return completedRuns.getInt(researchId);
    }

    public void setCompletedRuns(String researchId, int runs) {
        if (runs <= 0) completedRuns.removeInt(researchId);
        else completedRuns.put(researchId, runs);
    }

    public long getPartialCWU(String researchId) {
        return partialCWU.getLong(researchId);
    }

    public void setPartialCWU(String researchId, long cwu) {
        if (cwu <= 0) partialCWU.removeLong(researchId);
        else partialCWU.put(researchId, cwu);
    }

    public boolean isComplete(String researchId) {
        Research r = ResearchRegistry.get(researchId);
        return r != null && getCompletedRuns(researchId) >= r.getRunsRequired();
    }

    /** True if every prerequisite of this research is complete (so it can be started). */
    public boolean isUnlocked(String researchId) {
        Research r = ResearchRegistry.get(researchId);
        if (r == null) return false;
        for (String prereq : r.getPrerequisites()) {
            if (!isComplete(prereq)) return false;
        }
        return true;
    }

    /** 0.0 - 1.0 completion fraction including the partial in-progress run. */
    public float getProgress(String researchId) {
        Research r = ResearchRegistry.get(researchId);
        if (r == null || r.getRunsRequired() == 0) return 0f;
        float partial = r.getCwuPerRun() <= 0 ? 0f
                : Math.min(1f, (float) getPartialCWU(researchId) / r.getCwuPerRun());
        return Math.min(1f, (getCompletedRuns(researchId) + partial) / r.getRunsRequired());
    }

    public void clear() {
        completedRuns.clear();
        partialCWU.clear();
    }

    public void copyFrom(ResearchState other) {
        clear();
        completedRuns.putAll(other.completedRuns);
        partialCWU.putAll(other.partialCWU);
    }

    /** Merge another state in, keeping the highest completion seen for each research. */
    public void mergeFrom(ResearchState other) {
        for (Object2IntMap.Entry<String> e : other.completedRuns.object2IntEntrySet()) {
            if (e.getIntValue() > getCompletedRuns(e.getKey())) {
                completedRuns.put(e.getKey(), e.getIntValue());
            }
        }
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound runs = new NBTTagCompound();
        for (Object2IntMap.Entry<String> e : completedRuns.object2IntEntrySet()) {
            runs.setInteger(e.getKey(), e.getIntValue());
        }
        tag.setTag(KEY_RUNS, runs);
        NBTTagCompound partial = new NBTTagCompound();
        for (Object2LongMap.Entry<String> e : partialCWU.object2LongEntrySet()) {
            partial.setLong(e.getKey(), e.getLongValue());
        }
        tag.setTag(KEY_PARTIAL, partial);
        return tag;
    }

    public void deserializeNBT(NBTTagCompound tag) {
        clear();
        if (tag == null) return;
        NBTTagCompound runs = tag.getCompoundTag(KEY_RUNS);
        for (String key : runs.getKeySet()) {
            completedRuns.put(key, runs.getInteger(key));
        }
        NBTTagCompound partial = tag.getCompoundTag(KEY_PARTIAL);
        for (String key : partial.getKeySet()) {
            partialCWU.put(key, partial.getLong(key));
        }
    }
}
