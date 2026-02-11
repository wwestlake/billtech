package com.billtech.client.screen;

import com.billtech.menu.ItemControllerMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.billtech.network.ItemControllerSearchPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ItemControllerScreen extends AbstractContainerScreen<ItemControllerMenu> {
    private static final int SLOT_BORDER = 0xFF141414;
    private static final int SLOT_OUTER = 0xFF3A3A3A;
    private static final int SLOT_INNER = 0xFF1E1E1E;
    private static final int DISPLAY_SIZE = 45;
    private EditBox searchBox;
    private EditBox craftAmountBox;
    private Button craftButton;
    private Button craftMaxButton;

    public ItemControllerScreen(ItemControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int boxX = leftPos + 8;
        int boxY = topPos + 6;
        searchBox = new EditBox(font, boxX, boxY, 120, 12, Component.translatable("gui.billtech.search"));
        searchBox.setValue("");
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        int buttonY = topPos + 6;
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 132, buttonY, 18, 12).build());

        addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            }
        }).bounds(leftPos + 152, buttonY, 18, 12).build());

        int craftY = topPos + 124;
        craftAmountBox = new EditBox(font, leftPos + 8, craftY, 40, 12, Component.literal("Qty"));
        craftAmountBox.setValue("1");
        addRenderableWidget(craftAmountBox);
        craftButton = Button.builder(Component.literal("Craft"), btn -> sendCraft(false))
                .bounds(leftPos + 52, craftY, 46, 12)
                .build();
        craftMaxButton = Button.builder(Component.literal("Max"), btn -> sendCraft(true))
                .bounds(leftPos + 102, craftY, 32, 12)
                .build();
        addRenderableWidget(craftButton);
        addRenderableWidget(craftMaxButton);
    }

    private void onSearchChanged(String text) {
        ClientPlayNetworking.send(new ItemControllerSearchPayload(text));
    }

    private void sendCraft(boolean max) {
        ItemStack target = findFirstDisplayStack();
        if (target.isEmpty()) {
            return;
        }
        int amount = max ? Integer.MAX_VALUE : parseCraftAmount();
        if (amount <= 0) {
            return;
        }
        ClientPlayNetworking.send(new com.billtech.network.ItemControllerCraftPayload(target.copyWithCount(1), amount));
    }

    private ItemStack findFirstDisplayStack() {
        for (int i = 0; i < DISPLAY_SIZE; i++) {
            ItemStack stack = menu.getDisplayStack(i);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private int parseCraftAmount() {
        try {
            int value = Integer.parseInt(craftAmountBox.getValue().trim());
            return Math.max(1, value);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1B1B1B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
            graphics.fill(x, y, x + 16, y + 16, SLOT_OUTER);
            graphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_INNER);
        }
        String page = (menu.getPage() + 1) + "/" + menu.getPageCount();
        graphics.drawString(font, page, leftPos + 132, topPos + 20, 0xA0A0A0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
        graphics.drawString(font, "Craft (first result):", 8, 112, 0xA0A0A0, false);
    }
}
