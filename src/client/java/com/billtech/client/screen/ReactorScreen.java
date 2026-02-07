package com.billtech.client.screen;

import com.billtech.menu.ReactorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ReactorScreen extends MachineScreenBase<ReactorMenu> {
    public ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
