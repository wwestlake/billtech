package com.billtech.command;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("billtech")
                        .then(Commands.literal("book")
                                .executes(context -> giveGuideBook(context.getSource())))));
    }

    private static int giveGuideBook(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return 0;
        }
        ItemStack book = createGuideBook(player);
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        return 1;
    }

    private static ItemStack createGuideBook(ServerPlayer player) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, buildGuideContent(player));
        return book;
    }

    private static WrittenBookContent buildGuideContent(ServerPlayer player) {
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.literal(
                "BillTech Guide\n\n" +
                "Welcome to BillTech!\n\n" +
                "Use /billtech book\n" +
                "anytime to get\n" +
                "this guide.")));
        pages.add(Filterable.passThrough(Component.literal(
                "Tier 1 Focus\n\n" +
                "- Power\n" +
                "- Fluids\n" +
                "- Basic machines\n\n" +
                "Build a generator,\n" +
                "then a pyrolyzer\n" +
                "to start crude oil.")));
        pages.add(Filterable.passThrough(Component.literal(
                "Flow Basics\n\n" +
                "- Pipes move fluids\n" +
                "- Pumps push flow\n" +
                "- Tanks store 10B\n\n" +
                "Use the flow meter\n" +
                "to verify direction\n" +
                "and rate.")));
        pages.add(Filterable.passThrough(Component.literal(
                "Need Help?\n\n" +
                "Open the BillTech\n" +
                "wiki for recipes,\n" +
                "machine rules, and\n" +
                "side configuration.")));

        return new WrittenBookContent(
                Filterable.passThrough("BillTech Guide"),
                "BillTech",
                0,
                pages,
                true);
    }
}
