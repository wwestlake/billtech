package com.billtech.upgrade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.MenuProvider;

public class UpgradeItem extends Item {
    private final UpgradeType type;

    public UpgradeItem(Properties properties, UpgradeType type) {
        super(properties);
        this.type = type;
    }

    public UpgradeType getType() {
        return type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
        if (be instanceof MenuProvider provider) {
            context.getPlayer().openMenu(provider);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
