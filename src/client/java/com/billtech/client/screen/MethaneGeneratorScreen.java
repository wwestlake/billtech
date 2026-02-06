package com.billtech.client.screen;

import com.billtech.menu.MethaneGeneratorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MethaneGeneratorScreen extends MachineScreenBase<MethaneGeneratorMenu> {
    public MethaneGeneratorScreen(MethaneGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        long methane = menu.getInputAmount();
        long methaneBuf = menu.getInputBuffer();
        long energy = menu.getEnergyAmount();
        long energyCap = menu.getEnergyCapacity();
        long methaneRate = menu.getMethanePerTick();
        long energyRate = menu.getEnergyPerTick();
        graphics.drawString(font, "Methane: " + methane + " / " + methaneBuf + " mB", 8, 20, 0xCCCCCC, false);
        graphics.drawString(font, "Energy: " + energy + " / " + energyCap, 8, 32, 0xCCCCCC, false);
        graphics.drawString(font, "Rate: " + methaneRate + " mB/t", 8, 44, 0xCCCCCC, false);
        graphics.drawString(font, "Output: " + energyRate + " /t", 8, 56, 0xCCCCCC, false);
    }

    @Override
    protected boolean useDefaultStatusLines() {
        return false;
    }
}
