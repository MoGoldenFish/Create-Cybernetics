package com.perigrine3.createcybernetics.screen.custom.surgery;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.common.surgery.DefaultOrgans;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AvailableOrgansPanel {
    private static final int WIDTH = 82;
    private static final int HEIGHT = 150;
    private static final int TITLE_HEIGHT = 20;
    private static final int PADDING = 5;
    private static final int ITEM_SIZE = 18;
    private static final int COLUMNS = 4;
    private static final int VISIBLE_ROWS = 6;

    private final List<ItemStack> entries = new ArrayList<>();
    private Set<CyberwareSlot> displayedSlots = EnumSet.noneOf(CyberwareSlot.class);
    private int scrollRow;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public void render(GuiGraphics graphics, Font font, int menuLeft, int menuTop,
                       Collection<CyberwareSlot> slots, int mouseX, int mouseY) {
        updateEntries(slots);
        hoveredStack = ItemStack.EMPTY;

        int x = menuLeft - WIDTH - 4;
        int y = menuTop + 18;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF161B1C);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFF657175);
        graphics.fill(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, 0xE622292B);

        Component title = Component.translatable("gui.createcybernetics.surgery.available_organs");
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), WIDTH - 8), x + 4, y + 6, 0xFFE7E7E7, false);

        int gridX = x + PADDING;
        int gridY = y + TITLE_HEIGHT;
        int firstEntry = scrollRow * COLUMNS;
        int visibleCount = VISIBLE_ROWS * COLUMNS;

        graphics.enableScissor(gridX, gridY, x + WIDTH - PADDING, gridY + VISIBLE_ROWS * ITEM_SIZE);
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            int entryIndex = firstEntry + visibleIndex;
            if (entryIndex >= entries.size()) break;

            int itemX = gridX + (visibleIndex % COLUMNS) * ITEM_SIZE;
            int itemY = gridY + (visibleIndex / COLUMNS) * ITEM_SIZE;
            ItemStack stack = entries.get(entryIndex);
            boolean hovered = mouseX >= itemX && mouseX < itemX + 16
                    && mouseY >= itemY && mouseY < itemY + 16;

            if (hovered) {
                graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0x80FFFFFF);
                hoveredStack = stack;
            }

            graphics.renderItem(stack, itemX, itemY);
        }
        graphics.disableScissor();

        if (entries.isEmpty()) {
            Component empty = Component.translatable("gui.createcybernetics.surgery.no_available_organs");
            graphics.drawWordWrap(font, empty, gridX, gridY + 4, WIDTH - PADDING * 2, 0xFF8F999C);
        }

        renderScrollbar(graphics, x + WIDTH - 4, gridY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int menuLeft, int menuTop) {
        int x = menuLeft - WIDTH - 4;
        int y = menuTop + 18;
        if (mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + HEIGHT) {
            return false;
        }

        int maxScroll = maxScrollRow();
        if (scrollY < 0) {
            scrollRow = Math.min(maxScroll, scrollRow + 1);
        } else if (scrollY > 0) {
            scrollRow = Math.max(0, scrollRow - 1);
        }
        return true;
    }

    public ItemStack getHoveredStack() {
        return hoveredStack;
    }

    private void updateEntries(Collection<CyberwareSlot> slots) {
        EnumSet<CyberwareSlot> nextSlots = slots.isEmpty()
                ? EnumSet.noneOf(CyberwareSlot.class)
                : EnumSet.copyOf(slots);
        if (nextSlots.equals(displayedSlots)) return;

        displayedSlots = nextSlots;
        scrollRow = 0;
        entries.clear();

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (isAvailableForDisplayedSlot(stack)) {
                entries.add(stack);
            }
        }
    }

    private boolean isAvailableForDisplayedSlot(ItemStack stack) {
        for (CyberwareSlot slot : displayedSlots) {
            if (DefaultOrgans.isOrganForSlot(stack, slot)) return true;
            if (stack.getItem() instanceof ICyberwareItem cyberware
                    && cyberware.surgeryInstallable()
                    && cyberware.supportsSlot(slot)) {
                return true;
            }
        }
        return false;
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y) {
        int trackHeight = VISIBLE_ROWS * ITEM_SIZE;
        graphics.fill(x, y, x + 2, y + trackHeight, 0xFF0B0E0F);

        int totalRows = Math.max(1, (entries.size() + COLUMNS - 1) / COLUMNS);
        int thumbHeight = Math.max(10, trackHeight * VISIBLE_ROWS / Math.max(VISIBLE_ROWS, totalRows));
        int travel = trackHeight - thumbHeight;
        int thumbY = maxScrollRow() == 0 ? y : y + Math.round(travel * (scrollRow / (float) maxScrollRow()));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xFFB3C0C3);
    }

    private int maxScrollRow() {
        int totalRows = (entries.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, totalRows - VISIBLE_ROWS);
    }
}
