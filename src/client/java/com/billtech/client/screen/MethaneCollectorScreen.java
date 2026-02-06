package com.billtech.client.screen;

import com.billtech.menu.MethaneCollectorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MethaneCollectorScreen extends MachineScreenBase<MethaneCollectorMenu> {
    public MethaneCollectorScreen(MethaneCollectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        boolean biomeOk = menu.isInValidBiome();
        long stored = menu.getOutputAmount();
        long buffer = menu.getOutputBuffer();
        long rate = menu.getOutputPerTick();
        String status = biomeOk ? (stored >= buffer ? "Buffer Full" : "Collecting") : "No Swamp";
        graphics.drawString(font, "Biome: " + (biomeOk ? "Swamp" : "Other"), 8, 20, 0xCCCCCC, false);
        graphics.drawString(font, "Stored: " + stored + " / " + buffer + " mB", 8, 32, 0xCCCCCC, false);
        graphics.drawString(font, "Rate: " + rate + " mB/t", 8, 44, 0xCCCCCC, false);
        graphics.drawString(font, "Status: " + status, 8, 56, 0xCCCCCC, false);
    }

    @Override
    protected boolean useDefaultStatusLines() {
        return false;
    }
}
