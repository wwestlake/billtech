package com.billtech.client.screen;

import com.billtech.menu.EssenceExtractorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EssenceExtractorScreen extends MachineScreenBase<EssenceExtractorMenu> {
    public EssenceExtractorScreen(EssenceExtractorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        if (!isStatusTabActive()) {
            graphics.drawString(font, "S:Speed  M:Multi  Y:Yield", 8, 20, 0xAAAAAA, false);
        }
    }

    @Override
    protected boolean useDefaultStatusLines() {
        return true;
    }
}
