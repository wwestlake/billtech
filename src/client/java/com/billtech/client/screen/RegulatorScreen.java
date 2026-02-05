package com.billtech.client.screen;

import com.billtech.menu.RegulatorMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RegulatorScreen extends AbstractContainerScreen<RegulatorMenu> {
    private static final int SLIDER_WIDTH = 120;
    private static final int SLIDER_HEIGHT = 8;

    public RegulatorScreen(RegulatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 88;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1B1B1B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);

        int sliderX = leftPos + 28;
        int sliderY = topPos + 40;
        graphics.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + SLIDER_HEIGHT, 0xFF151515);
        graphics.fill(sliderX + 1, sliderY + 1, sliderX + SLIDER_WIDTH - 1, sliderY + SLIDER_HEIGHT - 1, 0xFF3A3A3A);

        int percent = menu.getTargetPercent();
        int knobX = sliderX + (percent * (SLIDER_WIDTH - 6)) / 100;
        graphics.fill(knobX, sliderY - 2, knobX + 6, sliderY + SLIDER_HEIGHT + 2, 0xFF4AA3FF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, "Target: " + menu.getTargetPercent() + "%", 8, 22, 0xCCCCCC, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int sliderX = leftPos + 28;
            int sliderY = topPos + 40;
            if (mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH
                    && mouseY >= sliderY - 2 && mouseY <= sliderY + SLIDER_HEIGHT + 2) {
                int percent = (int) Math.round(((mouseX - sliderX) / (double) (SLIDER_WIDTH - 1)) * 100.0);
                percent = Math.max(0, Math.min(100, percent));
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, percent);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
