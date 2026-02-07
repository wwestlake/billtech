package com.billtech.client.screen;

import com.billtech.menu.CrackingTowerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CrackingTowerScreen extends MachineScreenBase<CrackingTowerMenu> {
    public CrackingTowerScreen(CrackingTowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
