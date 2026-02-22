package com.billtech.client.screen;

import com.billtech.menu.SteamEngineMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SteamEngineScreen extends MachineScreenBase<SteamEngineMenu> {
    public SteamEngineScreen(SteamEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, "Steam: " + menu.getInputAmount() + " / " + menu.getInputBuffer() + " mB", 8, 20, 0xCCCCCC, false);
        graphics.drawString(font, "Energy: " + menu.getEnergyAmount() + " / " + menu.getEnergyCapacity(), 8, 32, 0xCCCCCC, false);
        graphics.drawString(font, "Rate: " + menu.getSteamPerTick() + " mB/t", 8, 44, 0xCCCCCC, false);
        graphics.drawString(font, "Output: " + menu.getEnergyPerTick() + " /t", 8, 56, 0xCCCCCC, false);
    }

    @Override
    protected boolean useDefaultStatusLines() {
        return false;
    }
}
