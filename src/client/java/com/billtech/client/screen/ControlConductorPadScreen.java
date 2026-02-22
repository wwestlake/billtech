package com.billtech.client.screen;

import com.billtech.menu.ControlConductorPadMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ControlConductorPadScreen extends MachineScreenBase<ControlConductorPadMenu> {
    public ControlConductorPadScreen(ControlConductorPadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        if (isStatusTabActive()) {
            int captured = menu.getStatusFluidInStored();
            graphics.drawString(font, "Captured: " + (captured > 0 ? "Yes" : "No"), 8, 20, 0xD0D0D0, false);
            graphics.drawString(font, "Charge: " + menu.getStatusEnergyStored() + " / " + menu.getStatusEnergyCapacity(), 8, 30, 0xD0D0D0, false);
            graphics.drawString(font, "Process: " + menu.getStatusFluidOutStored() + " / " + menu.getStatusFluidOutCapacity(), 8, 40, 0xD0D0D0, false);
        } else {
            graphics.drawString(font, "Holds and conditions hostile mobs.", 8, 20, 0xAAAAAA, false);
        }
    }

    @Override
    protected boolean useDefaultStatusLines() {
        return false;
    }
}
