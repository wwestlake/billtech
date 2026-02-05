package com.billtech.client.screen;

import com.billtech.menu.PaperPressMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PaperPressScreen extends MachineScreenBase<PaperPressMenu> {
    public PaperPressScreen(PaperPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
