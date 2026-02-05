package com.billtech.client.screen;

import com.billtech.menu.OilExtractorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class OilExtractorScreen extends MachineScreenBase<OilExtractorMenu> {
    public OilExtractorScreen(OilExtractorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
