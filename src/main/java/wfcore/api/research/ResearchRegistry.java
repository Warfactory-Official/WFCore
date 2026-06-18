package wfcore.api.research;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global registry of every {@link Research}. Populated during mod init (Java or Groovy) via
 * {@link Research.Builder#register()}. Insertion order is preserved so the tree GUI lays nodes out stably.
 */
public final class ResearchRegistry {

    private static final Map<String, Research> RESEARCHES = new LinkedHashMap<>();

    private ResearchRegistry() {}

    public static void register(Research research) {
        if (research == null) return;
        RESEARCHES.put(research.getId(), research);
    }

    public static void unregister(String id) {
        if (id != null) RESEARCHES.remove(id);
    }

    @Nullable
    public static Research get(String id) {
        return id == null ? null : RESEARCHES.get(id);
    }

    public static boolean exists(String id) {
        return RESEARCHES.containsKey(id);
    }

    public static Collection<Research> all() {
        return Collections.unmodifiableCollection(RESEARCHES.values());
    }

    /** Researches with no prerequisites. */
    public static List<Research> roots() {
        List<Research> roots = new ArrayList<>();
        for (Research r : RESEARCHES.values()) {
            if (r.getPrerequisites().isEmpty()) roots.add(r);
        }
        return roots;
    }

    /** Researches that directly depend on the given research id. */
    public static List<Research> childrenOf(String id) {
        List<Research> children = new ArrayList<>();
        for (Research r : RESEARCHES.values()) {
            if (r.getPrerequisites().contains(id)) children.add(r);
        }
        return children;
    }

    /** Researches grouped under the given category, preserving registration order. */
    public static List<Research> byCategory(String category) {
        List<Research> list = new ArrayList<>();
        for (Research r : RESEARCHES.values()) {
            if (r.getCategory().equals(category)) list.add(r);
        }
        return list;
    }
}
