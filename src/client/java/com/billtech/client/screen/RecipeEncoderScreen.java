package com.billtech.client.screen;

import com.billtech.menu.RecipeEncoderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class RecipeEncoderScreen extends AbstractContainerScreen<RecipeEncoderMenu> {
    private static final int SLOT_BORDER = 0xFF141414;
    private static final int SLOT_OUTER = 0xFF3A3A3A;
    private static final int SLOT_INNER = 0xFF1E1E1E;
    private Button encodeButton;

    public RecipeEncoderScreen(RecipeEncoderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int buttonX = leftPos + 108;
        int buttonY = topPos + 18;
        encodeButton = Button.builder(Component.literal("Encode"), btn -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(buttonX, buttonY, 56, 16).build();
        addRenderableWidget(encodeButton);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1B1B1B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
            graphics.fill(x, y, x + 16, y + 16, SLOT_OUTER);
            graphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_INNER);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, "Pattern", 8, 22, 0xCCCCCC, false);
        graphics.drawString(font, "Plastic", 108, 38, 0xCCCCCC, false);
    }
}
