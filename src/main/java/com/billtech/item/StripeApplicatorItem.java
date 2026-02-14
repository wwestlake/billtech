package com.billtech.item;

import com.billtech.stripe.StripeCarrier;
import com.billtech.stripe.StripeData;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StripeApplicatorItem extends Item {
    public StripeApplicatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        boolean client = context.getLevel().isClientSide();
        BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(be instanceof StripeCarrier carrier)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isCrouching()) {
            if (!client) {
                carrier.setStripeData(StripeData.EMPTY);
            }
            return InteractionResult.SUCCESS;
        }
        BuildResult build = buildStripeData(player);
        if (build == null) {
            return InteractionResult.PASS;
        }
        StripeData data = build.data();
        StripeData current = carrier.getStripeData();
        if (current != null && current.signature() == data.signature()) {
            return InteractionResult.SUCCESS;
        }
        if (!client) {
            carrier.setStripeData(data);
            if (!player.getAbilities().instabuild) {
                build.consume(player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private BuildResult buildStripeData(Player player) {
        int[] colors = new int[]{-1, -1, -1};
        int count = 0;
        int invSize = player.getInventory().getContainerSize();
        for (int slot = 0; slot < invSize && count < 3; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof DyeItem dye) {
                colors[count] = dye.getDyeColor().getId();
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        int[] compact = new int[]{Math.max(colors[0], 0), Math.max(colors[1], 0), Math.max(colors[2], 0)};
        StripeData data = new StripeData(count, compact);
        return new BuildResult(data, colors);
    }

    private record BuildResult(StripeData data, int[] colors) {
        void consume(Player player) {
            int needed = data.stripeCount();
            int invSize = player.getInventory().getContainerSize();
            for (int slot = 0; slot < invSize && needed > 0; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.getItem() instanceof DyeItem dye) {
                    int color = dye.getDyeColor().getId();
                    for (int i = 0; i < data.stripeCount(); i++) {
                        if (colors[i] == color) {
                            stack.shrink(1);
                            colors[i] = -1;
                            needed--;
                            break;
                        }
                    }
                }
            }
        }
    }

}
