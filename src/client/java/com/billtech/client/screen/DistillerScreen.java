package com.billtech.client.screen;

import com.billtech.menu.DistillerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DistillerScreen extends MachineScreenBase<DistillerMenu> {
    public DistillerScreen(DistillerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
