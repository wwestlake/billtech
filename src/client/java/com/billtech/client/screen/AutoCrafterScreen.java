package com.billtech.client.screen;

import com.billtech.menu.AutoCrafterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AutoCrafterScreen extends AbstractContainerScreen<AutoCrafterMenu> {
    private static final int SLOT_BORDER = 0xFF141414;
    private static final int SLOT_OUTER = 0xFF3A3A3A;
    private static final int SLOT_INNER = 0xFF1E1E1E;
    private Button craftOneButton;
    private Button craftStackButton;
    private Button craftMaxButton;

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = topPos + 18;
        craftOneButton = Button.builder(Component.literal("Craft 1"), btn -> sendCraft(0))
                .bounds(leftPos + 106, buttonY, 58, 16)
                .build();
        craftStackButton = Button.builder(Component.literal("Craft 16"), btn -> sendCraft(1))
                .bounds(leftPos + 106, buttonY + 20, 58, 16)
                .build();
        craftMaxButton = Button.builder(Component.literal("Craft Max"), btn -> sendCraft(2))
                .bounds(leftPos + 106, buttonY + 40, 58, 16)
                .build();
        addRenderableWidget(craftOneButton);
        addRenderableWidget(craftStackButton);
        addRenderableWidget(craftMaxButton);
    }

    private void sendCraft(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
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
        renderRecipePattern(graphics);
    }

    private void renderRecipePattern(GuiGraphics graphics) {
        List<ItemStack> pattern = menu.getPattern();
        int startX = leftPos + 30;
        int startY = topPos + 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                if (index >= pattern.size()) {
                    continue;
                }
                ItemStack stack = pattern.get(index);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, startX + col * 18, startY + row * 18);
                    graphics.renderItemDecorations(font, stack, startX + col * 18, startY + row * 18);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, "Recipe", 8, 22, 0xCCCCCC, false);
        graphics.drawString(font, "Card", 8, 56, 0xCCCCCC, false);
    }
}
