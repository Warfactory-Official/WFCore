package wfcore.common.gui.research;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.IValue;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.scroll.HorizontalScrollData;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import net.minecraft.item.ItemStack;
import wfcore.api.research.Research;
import wfcore.api.research.ResearchRegistry;
import wfcore.api.research.ResearchState;
import wfcore.common.metatileentities.research.MetaTileEntityResearchUnit;

import java.util.List;
import java.util.function.Supplier;

/**
 * Minecraft-advancements-styled research tree. Left: a 2D drag-to-pan node graph (clicking a node only
 * selects it). Right: a detail panel with per-run + total CWU/EU/item costs (item sprites with vanilla
 * tooltips), unlocked-item sprites, a start/cancel button, and the 3-slot research queue.
 *
 * <p>Built on both sides, so it avoids client-only classes (uses {@link IKey}).
 */
public final class ResearchTreeGui {

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 230;

    private static final int CANVAS_X = 6;
    private static final int CANVAS_Y = 18;
    private static final int CANVAS_W = 196;
    private static final int CANVAS_H = 206;

    private static final int DETAIL_X = 206;
    private static final int DETAIL_W = PANEL_W - DETAIL_X - 6;

    private static final int NODE = 26;
    private static final int COL_SPACING = 42;
    private static final int ROW_SPACING = 36;
    private static final int MARGIN = 10;
    private static final int MAX_ITEM_SLOTS = 6;

    private static final int COLOR_LINE = 0xFF8A8A8A;
    private static final int COLOR_BORDER = 0xFF101010;
    private static final int COLOR_PANEL = 0xFF202024;
    private static final int COLOR_LOCKED = 0xFF555555;
    private static final int COLOR_AVAILABLE = 0xFF2F6BD8;
    private static final int COLOR_QUEUED = 0xFFB07818;
    private static final int COLOR_ACTIVE = 0xFFE0A020;
    private static final int COLOR_COMPLETE = 0xFF44A050;

    // client-side selection (one GUI per client)
    private static final String[] SELECTED = {null};

    private ResearchTreeGui() {}

    public static ModularPanel build(MetaTileEntityResearchUnit mte, PosGuiData data,
                                     PanelSyncManager syncManager, UISettings settings) {
        ModularPanel panel = ModularPanel.defaultPanel("research_unit", PANEL_W, PANEL_H);
        panel.child(new TextWidget<>(IKey.lang("wfcore.gui.research.title")).pos(8, 6));
        panel.child(new TextWidget<>(IKey.dynamic(() ->
                modeLabel(mte) + "  x" + mte.getJobCapacity())).pos(150, 6));

        if (!mte.isStructureFormed()) {
            panel.child(new TextWidget<>(IKey.lang("wfcore.gui.research.not_formed")).pos(CANVAS_X + 8, CANVAS_Y + 8));
            return panel;
        }
        if (mte.getMode() == MetaTileEntityResearchUnit.Mode.SLAVE) {
            panel.child(new TextWidget<>(IKey.lang("wfcore.gui.research.slave_mode")).pos(CANVAS_X + 8, CANVAS_Y + 8));
            return panel;
        }

        panel.child(buildCanvas(mte, syncManager));
        panel.child(buildDetail(mte, syncManager));
        return panel;
    }

    private static ScrollWidget<?> buildCanvas(MetaTileEntityResearchUnit mte, PanelSyncManager syncManager) {
        ScrollWidget<?> canvas = new ScrollWidget<>();
        canvas.getScrollArea().setScrollDataX(new HorizontalScrollData());
        canvas.getScrollArea().setScrollDataY(new VerticalScrollData());
        canvas.pos(CANVAS_X, CANVAS_Y).size(CANVAS_W, CANVAS_H);
        canvas.background(UITexture.builder()
                .location("minecraft:gui/advancements/backgrounds/stone")
                .imageSize(16, 16)
                .tiled()
                .build());

        int maxX = 0, maxY = 0;
        for (Research research : ResearchRegistry.all()) {
            maxX = Math.max(maxX, research.getGridX());
            maxY = Math.max(maxY, research.getGridY());
        }
        canvas.getScrollArea().getScrollX().setScrollSize(MARGIN * 2 + (maxX + 1) * COL_SPACING);
        canvas.getScrollArea().getScrollY().setScrollSize(MARGIN * 2 + (maxY + 1) * ROW_SPACING);

        ResearchState state = mte.getResearchState();
        for (Research research : ResearchRegistry.all()) {
            for (String prereqId : research.getPrerequisites()) {
                Research prereq = ResearchRegistry.get(prereqId);
                if (prereq != null) addConnector(canvas, prereq, research);
            }
        }
        int index = 0;
        for (Research research : ResearchRegistry.all()) {
            canvas.child(buildNode(mte, state, research, index++, syncManager));
        }
        return canvas;
    }

    private static ButtonWidget<?> buildNode(MetaTileEntityResearchUnit mte, ResearchState state,
                                             Research research, int index, PanelSyncManager syncManager) {
        final String rid = research.getId();

        InteractionSyncHandler select = new InteractionSyncHandler().setOnMousePressed(d -> mte.setSelected(rid));
        syncManager.syncValue("research_node", index, select);

        ButtonWidget<?> node = new ButtonWidget<>();
        node.pos(nodeX(research), nodeY(research)).size(NODE, NODE);
        node.background(new Rectangle().setColor(nodeColor(mte, state, rid)));
        node.backgroundOverlay(new Rectangle().setColor(COLOR_BORDER).hollow(1f));
        if (!research.getIcon().isEmpty()) {
            Widget<?> icon = new ItemDisplayWidget().item(research.getIcon());
            icon.pos(4, 4).size(18);
            node.child(icon);
        }
        node.addTooltipLine(IKey.lang(research.getNameKey()));
        node.addTooltipLine(IKey.lang("wfcore.gui.research.runs", research.getRunsRequired()));
        node.addTooltipLine(IKey.lang("wfcore.gui.research.cwu_per_run", research.getCwuPerRun()));
        node.addTooltipLine(IKey.lang("wfcore.gui.research.select_hint"));
        node.onMousePressed(mb -> {
            SELECTED[0] = rid;
            return false; // let the sync handler fire the server-side selection
        });
        node.syncHandler("research_node", index);
        return node;
    }

    private static void addConnector(ScrollWidget<?> canvas, Research from, Research to) {
        int x1 = nodeX(from) + NODE / 2;
        int y1 = nodeY(from) + NODE / 2;
        int x2 = nodeX(to) + NODE / 2;
        int y2 = nodeY(to) + NODE / 2;
        int hx = Math.min(x1, x2);
        int hw = Math.max(2, Math.abs(x2 - x1));
        canvas.child(new Widget<>().background(new Rectangle().setColor(COLOR_LINE)).pos(hx, y1 - 1).size(hw, 2));
        int vy = Math.min(y1, y2);
        int vh = Math.max(2, Math.abs(y2 - y1));
        canvas.child(new Widget<>().background(new Rectangle().setColor(COLOR_LINE)).pos(x2 - 1, vy).size(2, vh));
    }

    private static ParentWidget<?> buildDetail(MetaTileEntityResearchUnit mte, PanelSyncManager syncManager) {
        ParentWidget<?> detail = new ParentWidget<>();
        detail.pos(DETAIL_X, CANVAS_Y).size(DETAIL_W, CANVAS_H);
        detail.background(new Rectangle().setColor(COLOR_PANEL));

        detail.child(new TextWidget<>(IKey.dynamicKey(() -> {
            Research r = selected();
            return r == null ? IKey.lang("wfcore.gui.research.select_hint") : IKey.lang(r.getNameKey());
        })).pos(4, 3));

        detail.child(new TextWidget<>(IKey.lang("wfcore.gui.research.per_run")).pos(4, 15));
        addItemRow(detail, 4, 25, i -> inputPerRunAt(i));
        detail.child(new TextWidget<>(IKey.dynamic(() -> {
            Research r = selected();
            return r == null ? "" : "CWU " + r.getCwuPerRun() + " | EU/t " + r.getEut();
        })).pos(4, 45));

        detail.child(new TextWidget<>(IKey.lang("wfcore.gui.research.total")).pos(4, 57));
        addItemRow(detail, 4, 67, i -> inputTotalAt(i));
        detail.child(new TextWidget<>(IKey.dynamic(() -> {
            Research r = selected();
            return r == null ? "" : "CWU " + r.getTotalCWU() + " | EU " + r.getTotalEU();
        })).pos(4, 87));

        detail.child(new TextWidget<>(IKey.lang("wfcore.gui.research.unlocks")).pos(4, 99));
        addItemRow(detail, 4, 109, i -> unlockedAt(i));

        detail.child(new TextWidget<>(IKey.dynamic(() -> statusLine(mte))).pos(4, 130));

        InteractionSyncHandler action = new InteractionSyncHandler().setOnMousePressed(d -> mte.toggleSelected());
        syncManager.syncValue("research_action", 0, action);
        ButtonWidget<?> button = new ButtonWidget<>();
        button.pos(4, 142).size(DETAIL_W - 8, 14).syncHandler("research_action", 0);
        button.overlay(IKey.dynamicKey(() -> {
            Research r = selected();
            if (r != null && mte.isQueuedClient(r.getId())) return IKey.lang("wfcore.gui.research.cancel");
            return IKey.lang("wfcore.gui.research.start");
        }));
        detail.child(button);

        detail.child(new TextWidget<>(IKey.lang("wfcore.gui.research.queue")).pos(4, 162));
        detail.child(new TextWidget<>(IKey.dynamicKey(() -> queueKey(mte, 0))).pos(4, 174));
        detail.child(new TextWidget<>(IKey.dynamicKey(() -> queueKey(mte, 1))).pos(4, 184));
        detail.child(new TextWidget<>(IKey.dynamicKey(() -> queueKey(mte, 2))).pos(4, 194));
        return detail;
    }

    private static void addItemRow(ParentWidget<?> detail, int x, int y, java.util.function.IntFunction<ItemStack> provider) {
        for (int i = 0; i < MAX_ITEM_SLOTS; i++) {
            final int idx = i;
            ItemDisplayWidget sprite = new ItemDisplayWidget()
                    .item(itemValue(() -> provider.apply(idx)))
                    .displayAmount(true);
            sprite.pos(x + i * 18, y).size(18);
            sprite.tooltip(tt -> tt.setAutoUpdate(true).tooltipBuilder(rt -> {
                ItemStack s = provider.apply(idx);
                if (s != null && !s.isEmpty()) rt.addFromItem(s);
            }));
            detail.child(sprite);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Research selected() {
        return SELECTED[0] == null ? null : ResearchRegistry.get(SELECTED[0]);
    }

    private static ItemStack inputPerRunAt(int i) {
        Research r = selected();
        if (r == null) return ItemStack.EMPTY;
        List<ItemStack> items = r.getItemsPerRun();
        return i < items.size() ? items.get(i) : ItemStack.EMPTY;
    }

    private static ItemStack inputTotalAt(int i) {
        Research r = selected();
        if (r == null) return ItemStack.EMPTY;
        List<ItemStack> items = r.getItemsPerRun();
        if (i >= items.size()) return ItemStack.EMPTY;
        ItemStack total = items.get(i).copy();
        total.setCount(total.getCount() * r.getRunsRequired());
        return total;
    }

    private static ItemStack unlockedAt(int i) {
        Research r = selected();
        if (r == null) return ItemStack.EMPTY;
        List<ItemStack> items = r.getUnlockedItems();
        return i < items.size() ? items.get(i) : ItemStack.EMPTY;
    }

    private static IValue<ItemStack> itemValue(Supplier<ItemStack> supplier) {
        return new IValue<ItemStack>() {
            @Override
            public ItemStack getValue() {
                ItemStack s = supplier.get();
                return s == null ? ItemStack.EMPTY : s;
            }

            @Override
            public void setValue(ItemStack value) {}

            @Override
            public Class<ItemStack> getValueType() {
                return ItemStack.class;
            }
        };
    }

    private static String statusLine(MetaTileEntityResearchUnit mte) {
        Research r = selected();
        if (r == null) return "";
        ResearchState state = mte.getResearchState();
        String id = r.getId();
        int pct = Math.round(state.getProgress(id) * 100f);
        if (state.isComplete(id)) return "COMPLETE";
        if (mte.isActiveClient(id)) return "Researching " + pct + "%";
        if (mte.isQueuedClient(id)) return "Queued " + pct + "%";
        if (!state.isUnlocked(id)) return "Locked";
        return "Ready (" + pct + "%)";
    }

    private static IKey queueKey(MetaTileEntityResearchUnit mte, int slot) {
        List<String> queue = mte.getClientQueue();
        if (slot >= queue.size()) return IKey.str((slot + 1) + ". -");
        String id = queue.get(slot);
        Research r = ResearchRegistry.get(id);
        int pct = Math.round(mte.getClientProgress(id) * 100f);
        String marker = slot < mte.getJobCapacity() ? "*" : " "; // * = actively running
        IKey name = r != null ? IKey.lang(r.getNameKey()) : IKey.str(id);
        return IKey.comp(IKey.str((slot + 1) + marker + " "), name, IKey.str(" " + pct + "%"));
    }

    private static String modeLabel(MetaTileEntityResearchUnit mte) {
        return mte.getMode() == MetaTileEntityResearchUnit.Mode.CONTROL ? "CONTROL" : "SLAVE";
    }

    private static int nodeX(Research research) {
        return MARGIN + research.getGridX() * COL_SPACING;
    }

    private static int nodeY(Research research) {
        return MARGIN + research.getGridY() * ROW_SPACING;
    }

    private static int nodeColor(MetaTileEntityResearchUnit mte, ResearchState state, String rid) {
        if (state.isComplete(rid)) return COLOR_COMPLETE;
        if (mte.isActiveClient(rid)) return COLOR_ACTIVE;
        if (mte.isQueuedClient(rid)) return COLOR_QUEUED;
        if (state.isUnlocked(rid)) return COLOR_AVAILABLE;
        return COLOR_LOCKED;
    }
}
