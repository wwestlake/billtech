package com.billtech.client.screen;

import com.billtech.menu.SeparatorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SeparatorScreen extends MachineScreenBase<SeparatorMenu> {
    public SeparatorScreen(SeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
