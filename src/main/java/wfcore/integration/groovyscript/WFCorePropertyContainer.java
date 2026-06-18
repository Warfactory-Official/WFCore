package wfcore.integration.groovyscript;

import com.cleanroommc.groovyscript.compat.mods.GroovyPropertyContainer;

/**
 * Holds WFCore's GroovyScript-exposed registries. The {@code research} field becomes
 * {@code mods.wfcore.research} (the registry's "research" alias).
 */
public class WFCorePropertyContainer extends GroovyPropertyContainer {

    public final ResearchScriptRegistry research = new ResearchScriptRegistry();

    public WFCorePropertyContainer() {
        addProperty(research);
    }
}
