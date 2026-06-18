package wfcore.integration.groovyscript;

import com.cleanroommc.groovyscript.api.GroovyPlugin;
import com.cleanroommc.groovyscript.compat.mods.GroovyContainer;
import com.cleanroommc.groovyscript.compat.mods.GroovyPropertyContainer;
import org.jetbrains.annotations.NotNull;
import wfcore.Reference;

/**
 * GroovyScript compat entry point. Auto-discovered by GroovyScript via classpath scanning; exposes WFCore's
 * registries under {@code mods.wfcore.*}. Only loaded when GroovyScript is present (the class references
 * GroovyScript types, so it is never loaded otherwise).
 */
public class WFCoreGroovyPlugin implements GroovyPlugin {

    @Override
    public @NotNull String getModId() {
        return Reference.MODID;
    }

    @Override
    public @NotNull String getContainerName() {
        return Reference.MODNAME;
    }

    @Override
    public void onCompatLoaded(GroovyContainer<?> container) {}

    @Override
    public GroovyPropertyContainer createGroovyPropertyContainer() {
        return new WFCorePropertyContainer();
    }
}
