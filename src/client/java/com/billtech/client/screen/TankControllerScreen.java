package com.billtech.client.screen;

import com.billtech.menu.TankControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;

public class TankControllerScreen extends AbstractContainerScreen<TankControllerMenu> {
    public TankControllerScreen(TankControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 88;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1B1B1B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);
        int barX = leftPos + imageWidth - 20;
        int barY = topPos + 8;
        int barHeight = imageHeight - 16;
        graphics.fill(barX, barY, barX + 10, barY + barHeight, 0xFF1A1A1A);
        long amount = menu.getAmount();
        long capacity = menu.getCapacity();
        if (capacity > 0 && amount > 0) {
            int fill = (int) Math.min(barHeight, (amount * barHeight) / capacity);
            graphics.fill(barX + 1, barY + barHeight - fill, barX + 9, barY + barHeight, 0xFF3CA4FF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        long amount = menu.getAmount();
        long capacity = menu.getCapacity();
        long percent = capacity > 0 ? (amount * 100) / capacity : 0;
        int fluidId = menu.getFluidId();
        Component fluidName = Component.literal("None");
        if (fluidId >= 0) {
            var fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != null && fluid != Fluids.EMPTY) {
                var key = BuiltInRegistries.FLUID.getKey(fluid);
                fluidName = Component.literal(key == null ? "Unknown" : key.toString());
            }
        }
        graphics.drawString(font, "Stored: " + amount + " mB", 8, 26, 0xCCCCCC, false);
        graphics.drawString(font, "Capacity: " + capacity + " mB", 8, 38, 0xCCCCCC, false);
        graphics.drawString(font, "Fill: " + percent + "%", 8, 50, 0xCCCCCC, false);
        graphics.drawString(font, "Fluid: " + fluidName.getString(), 8, 62, 0xCCCCCC, false);
    }
}
