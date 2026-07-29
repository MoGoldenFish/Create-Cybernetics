package com.perigrine3.createcybernetics.client.gui;

import com.perigrine3.createcybernetics.network.payload.CyberwareTogglePayloads;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InventoryCyberwareToggleButton extends AbstractButton {

    public static final int SIZE = 10;

    private final InventoryCyberwareToggleEntries.Entry entry;

    public InventoryCyberwareToggleButton(int x, int y, InventoryCyberwareToggleEntries.Entry entry) {
        super(x, y, SIZE, SIZE, entry.icon().getHoverName());
        this.entry = entry;
        setTooltip(Tooltip.create(tooltipText()));
    }

    @Override
    public void onPress() {
        if (!entry.canToggle()) {
            openScreen();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.hasData(ModAttachments.CYBERWARE)) return;

        var data = minecraft.player.getData(ModAttachments.CYBERWARE);
        boolean desiredEnabled = !InventoryCyberwareToggleEntries.isEnabled(entry);

        for (InventoryCyberwareToggleEntries.Target target : entry.targets()) {
            if (data.isEnabled(target.slot(), target.index()) != desiredEnabled) {
                PacketDistributor.sendToServer(new CyberwareTogglePayloads.ToggleCyberwarePayload(
                        target.slot().name(), target.index()
                ));
            }
        }

        setTooltip(Tooltip.create(tooltipText()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && entry.canOpen() && clicked(mouseX, mouseY)) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            openScreen();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openScreen() {
        if (entry.openAction() != null) {
            entry.openAction().execute();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        boolean enabled = entry.canToggle() && InventoryCyberwareToggleEntries.isEnabled(entry);

        graphics.fill(x, y, x + width, y + height, isHoveredOrFocused() ? 0xFF8A8A8A : 0xFF555555);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF202020);

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 1.0F, y + 1.0F, 100.0F);
        pose.scale(0.5F, 0.5F, 1.0F);
        graphics.renderItem(entry.icon(), 0, 0);
        pose.popPose();

        if (entry.canToggle() && !enabled) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x88000000);
        }

        if (entry.canToggle()) {
            int stateColor = enabled ? 0xFF55FF55 : 0xFFFF5555;
            graphics.fill(x + width - 3, y + height - 3, x + width - 1, y + height - 1, stateColor);
        } else if (entry.canOpen()) {
            graphics.fill(x + width - 3, y + height - 3, x + width - 1, y + height - 1, 0xFF55AAFF);
        }

        if (!this.active) {
            graphics.fill(x, y, x + width, y + height, 0x99000000);
        }

        setTooltip(Tooltip.create(tooltipText()));
    }

    private Component tooltipText() {
        if (!entry.canToggle()) {
            return Component.translatable(
                "gui.createcybernetics.cyberware_button.open",
                entry.icon().getHoverName()
            );
        }

        if (entry.canOpen()) {
            return Component.translatable(
                InventoryCyberwareToggleEntries.isEnabled(entry)
                    ? "gui.createcybernetics.cyberware_button.enabled_open"
                    : "gui.createcybernetics.cyberware_button.disabled_open",
                entry.icon().getHoverName()
            );
        }

        return Component.translatable(
                InventoryCyberwareToggleEntries.isEnabled(entry)
                        ? "gui.createcybernetics.cyberware_toggle.enabled"
                        : "gui.createcybernetics.cyberware_toggle.disabled",
                entry.icon().getHoverName()
        );
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
