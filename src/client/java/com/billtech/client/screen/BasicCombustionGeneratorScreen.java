package com.billtech.client.screen;

import com.billtech.menu.BasicCombustionGeneratorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BasicCombustionGeneratorScreen extends MachineScreenBase<BasicCombustionGeneratorMenu> {
    public BasicCombustionGeneratorScreen(BasicCombustionGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
