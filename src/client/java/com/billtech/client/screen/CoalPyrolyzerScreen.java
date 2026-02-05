package com.billtech.client.screen;

import com.billtech.menu.CoalPyrolyzerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CoalPyrolyzerScreen extends MachineScreenBase<CoalPyrolyzerMenu> {
    public CoalPyrolyzerScreen(CoalPyrolyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
