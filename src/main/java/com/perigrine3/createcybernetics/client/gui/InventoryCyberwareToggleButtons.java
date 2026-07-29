package com.perigrine3.createcybernetics.client.gui;

import com.perigrine3.createcybernetics.network.payload.CyberwareTogglePayloads;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

public final class InventoryCyberwareToggleButtons {

    private static final int GAP = 2;
    private static final int GUI_HEIGHT = 166;

    private InventoryCyberwareToggleButtons() {}

    public static void addTo(AbstractContainerScreen<?> screen, Consumer<AbstractWidget> addWidget) {
        PacketDistributor.sendToServer(new CyberwareTogglePayloads.RequestToggleStatesPayload());

        int step = InventoryCyberwareToggleButton.SIZE + GAP;
        int buttonsPerColumn = Math.max(1, (GUI_HEIGHT - 8) / step);
        int baseX = screen.getGuiLeft() - InventoryCyberwareToggleButton.SIZE - GAP;
        int baseY = screen.getGuiTop() + 8;

        var entries = InventoryCyberwareToggleEntries.collect();
        for (int i = 0; i < entries.size(); i++) {
            int column = i / buttonsPerColumn;
            int row = i % buttonsPerColumn;
            int x = baseX - column * step;
            int y = baseY + row * step;

            addWidget.accept(new InventoryCyberwareToggleButton(x, y, entries.get(i)));
        }
    }
}
