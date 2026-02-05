package com.billtech.client.screen;

import com.billtech.menu.UpgradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class UpgradeScreen extends AbstractContainerScreen<UpgradeMenu> {
    public UpgradeScreen(UpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1B1B1B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);
        graphics.drawString(font, "Upgrades", leftPos + 8, topPos + 6, 0xFFFFFF, false);

        int gridX = leftPos + 62;
        int gridY = topPos + 20;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int x = gridX + col * 18;
                int y = gridY + row * 18;
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF141414);
                graphics.fill(x, y, x + 16, y + 16, 0xFF3A3A3A);
                graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF1E1E1E);
            }
        }

        int invX = leftPos + 8;
        int invY = topPos + 51;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invX + col * 18;
                int y = invY + row * 18;
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF141414);
                graphics.fill(x, y, x + 16, y + 16, 0xFF3A3A3A);
                graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF1E1E1E);
            }
        }
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            int x = invX + col * 18;
            int y = hotbarY;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF141414);
            graphics.fill(x, y, x + 16, y + 16, 0xFF3A3A3A);
            graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF1E1E1E);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
    }
}
