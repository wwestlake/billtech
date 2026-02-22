package com.billtech.client.screen;

import com.billtech.menu.TeslaCoilMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TeslaCoilScreen extends MachineScreenBase<TeslaCoilMenu> {
    public TeslaCoilScreen(TeslaCoilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        if (isStatusTabActive()) {
            graphics.drawString(font, "Stored: " + menu.getStatusEnergyStored() + " / " + menu.getStatusEnergyCapacity(), 8, 20, 0xD0D0D0, false);
            graphics.drawString(font, "Cooldown: " + menu.getStatusFluidInStored() + " / " + menu.getStatusFluidInCapacity(), 8, 30, 0xD0D0D0, false);
        } else {
            graphics.drawString(font, "Zaps nearest charged pad target.", 8, 20, 0xAAAAAA, false);
        }
    }

    @Override
    protected boolean useDefaultStatusLines() {
        return false;
    }
}
