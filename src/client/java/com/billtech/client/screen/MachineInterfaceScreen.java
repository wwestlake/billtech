package com.billtech.client.screen;

import com.billtech.menu.MachineInterfaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MachineInterfaceScreen extends AbstractContainerScreen<MachineInterfaceMenu> {
    public MachineInterfaceScreen(MachineInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Enable"), btn -> sendButton(MachineInterfaceMenu.ACTION_ENABLE))
                .bounds(leftPos + 26, topPos + 20, 50, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Disable"), btn -> sendButton(MachineInterfaceMenu.ACTION_DISABLE))
                .bounds(leftPos + 102, topPos + 20, 50, 20)
                .build());
    }

    private void sendButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1B1B1B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF141414);
            graphics.fill(x, y, x + 16, y + 16, 0xFF3A3A3A);
            graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF1E1E1E);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        String cardLabel = menu.getCardLabel();
        if (cardLabel.isBlank()) {
            cardLabel = "No Card";
        }
        graphics.drawString(font, "Card: " + cardLabel, 8, 46, 0xD0D0D0, false);
        String machine = "No Target";
        if (menu.hasTargetMachine()) {
            var block = BuiltInRegistries.BLOCK.byId(menu.getTargetBlockId());
            machine = block == null ? "Unknown" : block.getName().getString();
        }
        graphics.drawString(font, "Target: " + machine, 8, 56, 0xD0D0D0, false);
        String status = menu.hasTargetMachine()
                ? (menu.isRemoteEnabled() ? "Enabled" : "Disabled")
                : "Missing";
        graphics.drawString(font, "Remote: " + status, 8, 66, 0xD0D0D0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
