package com.jagtaczarmor.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

public class PlateClientTooltipComponent implements ClientTooltipComponent {
    private final PlateTooltipComponent data;

    public PlateClientTooltipComponent(PlateTooltipComponent data) {
        this.data = data;
    }

    public int getHeight() {
        return 19;
    }

    public int getWidth(Font font) {
        String durStr = String.format("%d/%d", this.data.getCurDurability(), this.data.getMaxDurability());
        return 78 + font.width(durStr);
    }

    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        ItemStack plateStack = this.data.getPlateStack();
        if (!plateStack.isEmpty()) {
            int itemX = x + 10;
            int itemY = y + 1;
            guiGraphics.renderItem(plateStack, itemX, itemY);
            int barX = itemX + 20;
            int barY = itemY + 6;
            int barWidth = 40;
            int barHeight = 5;
            int maxDur = this.data.getMaxDurability();
            int curDur = this.data.getCurDurability();
            float fillRatio = maxDur > 0 ? (float) curDur / (float) maxDur : 0.0F;
            int fillWidth = Math.round(fillRatio * (float) (barWidth - 2));
            fillWidth = Math.max(0, Math.min(barWidth - 2, fillWidth));
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, -16777216);
            guiGraphics.fill(barX + 1, barY + 1, barX + barWidth - 1, barY + barHeight - 1, -12303292);
            if (fillWidth > 0) {
                guiGraphics.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + barHeight - 1, -16733441);
            }
            String durStr = String.format("%d/%d", curDur, maxDur);
            guiGraphics.drawString(font, durStr, barX + barWidth + 6, barY - 2, -22016, false);
        }
    }
}