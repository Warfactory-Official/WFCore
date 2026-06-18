package wfcore.common.gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wfcore.Reference;

/**
 * Convenience {@link IGuiHolder} for WFCore MetaTileEntities, tagging screens with the wfcore mod id
 * so themes/keybinds resolve correctly. Implementors only need to provide {@code buildUI(...)}.
 */
public interface IWFGuiHolder extends IGuiHolder<PosGuiData> {

    @SideOnly(Side.CLIENT)
    @Override
    default ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(Reference.MODID, mainPanel);
    }
}
