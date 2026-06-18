package wfcore.integration.groovyscript;

import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import wfcore.api.research.Research;
import wfcore.api.research.ResearchRegistry;

import java.util.Collections;

/**
 * GroovyScript-facing registry exposed as {@code mods.wfcore.research}. Scripted researches are tracked so
 * they are cleanly removed on {@code /reload} and re-applied when scripts re-run.
 *
 * <pre>{@code
 * // config/groovyscript/postInit/research.groovy
 * mods.wfcore.research.add(
 *     mods.wfcore.research.builder('my_research')
 *         .icon(item('minecraft:diamond'))
 *         .pos(3, 0)
 *         .requires('fire_control_systems')
 *         .runs(50).cwuPerRun(800).eut(64).ticksPerRun(40)
 *         .itemPerRun(item('minecraft:diamond'))
 *         .blueprint())
 *
 * mods.wfcore.research.remove('sensor_arrays')
 * }</pre>
 */
public class ResearchScriptRegistry extends VirtualizedRegistry<Research> {

    public ResearchScriptRegistry() {
        super(Collections.singletonList("research"));
    }

    @Override
    public void onReload() {
        removeScripted().forEach(r -> ResearchRegistry.unregister(r.getId()));
        restoreFromBackup().forEach(ResearchRegistry::register);
    }

    /** Returns a fresh builder; pass the result (or its {@code .build()}) to {@link #add}. */
    public Research.Builder builder(String id) {
        return Research.builder(id);
    }

    public Research add(Research.Builder builder) {
        return add(builder.build());
    }

    public Research add(Research research) {
        if (research == null) {
            GroovyLog.get().error("wfcore research cannot be null");
            return null;
        }
        addScripted(research);
        ResearchRegistry.register(research);
        return research;
    }

    public boolean remove(String id) {
        Research research = ResearchRegistry.get(id);
        if (research == null) {
            GroovyLog.get().error("wfcore research '{}' does not exist", id);
            return false;
        }
        addBackup(research);
        ResearchRegistry.unregister(id);
        return true;
    }
}
