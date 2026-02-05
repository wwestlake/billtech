package com.billtech.client.screen;

import com.billtech.block.entity.PortMode;
import com.billtech.menu.MachineMenuBase;
import com.billtech.transport.TransportType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.EnumSet;

public abstract class MachineScreenBase<T extends MachineMenuBase> extends AbstractContainerScreen<T> {
    private static final int SLOT_BORDER = 0xFF141414;
    private static final int SLOT_OUTER = 0xFF3A3A3A;
    private static final int SLOT_INNER = 0xFF1E1E1E;
    private static final int SIDE_BORDER = 0xFF101010;
    private static final int SIDE_NONE = 0xFF3A3A3A;
    private static final int SIDE_INPUT = 0xFF2E6FD1;
    private static final int SIDE_OUTPUT = 0xFFD13B3B;
    private static final int SIDE_BOTH = 0xFF3BD166;

    private boolean showConfig;
    private TransportType currentType;
    private Button configButton;
    private Button itemButton;
    private Button fluidButton;
    private Button energyButton;

    protected MachineScreenBase(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        currentType = defaultTransport();
        int buttonY = topPos + 20;
        configButton = Button.builder(Component.literal("IO"), button -> toggleConfig())
                .pos(leftPos + imageWidth - 28, buttonY)
                .size(20, 14)
                .build();
        addRenderableWidget(configButton);

        itemButton = buildTransportButton(TransportType.ITEM, leftPos + 8, buttonY);
        fluidButton = buildTransportButton(TransportType.FLUID, leftPos + 30, buttonY);
        energyButton = buildTransportButton(TransportType.ENERGY, leftPos + 52, buttonY);
        addRenderableWidget(itemButton);
        addRenderableWidget(fluidButton);
        addRenderableWidget(energyButton);
        updateTransportButtons();
    }

    private Button buildTransportButton(TransportType type, int x, int y) {
        return Button.builder(Component.literal(labelFor(type)), button -> {
            currentType = type;
            updateTransportButtons();
        }).pos(x, y).size(20, 14).build();
    }

    private String labelFor(TransportType type) {
        return switch (type) {
            case ITEM -> "I";
            case FLUID -> "F";
            case ENERGY -> "E";
        };
    }

    private void toggleConfig() {
        showConfig = !showConfig;
        updateTransportButtons();
    }

    private void updateTransportButtons() {
        boolean show = showConfig;
        itemButton.visible = show && menu.supportsTransport(TransportType.ITEM);
        fluidButton.visible = show && menu.supportsTransport(TransportType.FLUID);
        energyButton.visible = show && menu.supportsTransport(TransportType.ENERGY);
        if (!menu.supportsTransport(currentType)) {
            currentType = defaultTransport();
        }
    }

    private TransportType defaultTransport() {
        if (menu.supportsTransport(TransportType.ITEM)) {
            return TransportType.ITEM;
        }
        if (menu.supportsTransport(TransportType.FLUID)) {
            return TransportType.FLUID;
        }
        return TransportType.ENERGY;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF1B1B1B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF2B2B2B);

        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
            graphics.fill(x, y, x + 16, y + 16, SLOT_OUTER);
            graphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_INNER);
        }

        if (showConfig) {
            renderSideConfig(graphics);
        }
    }

    private void renderSideConfig(GuiGraphics graphics) {
        int baseX = leftPos + imageWidth - 70;
        int baseY = topPos + 42;
        int size = 14;
        int step = 18;
        Direction facing = menu.getFacing();
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        drawSideSquare(graphics, baseX + step, baseY, size, menu.getSideMode(currentType, Direction.UP));
        drawSideSquare(graphics, baseX, baseY + step, size, menu.getSideMode(currentType, left));
        drawSideSquare(graphics, baseX + step, baseY + step, size, menu.getSideMode(currentType, facing));
        drawSideSquare(graphics, baseX + step * 2, baseY + step, size, menu.getSideMode(currentType, right));
        drawSideSquare(graphics, baseX + step, baseY + step * 2, size, menu.getSideMode(currentType, Direction.DOWN));
        drawSideSquare(graphics, baseX + step * 2, baseY + step * 2, size, menu.getSideMode(currentType, back));
    }

    private void drawSideSquare(GuiGraphics graphics, int x, int y, int size, PortMode mode) {
        int fill = switch (mode) {
            case NONE -> SIDE_NONE;
            case INPUT -> SIDE_INPUT;
            case OUTPUT -> SIDE_OUTPUT;
            case BOTH -> SIDE_BOTH;
        };
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, SIDE_BORDER);
        graphics.fill(x, y, x + size, y + size, fill);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showConfig && handleSideClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSideClick(double mouseX, double mouseY) {
        int baseX = leftPos + imageWidth - 70;
        int baseY = topPos + 42;
        int size = 14;
        int step = 18;
        Direction facing = menu.getFacing();
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        if (trySideButton(mouseX, mouseY, baseX + step, baseY, size, Direction.UP)) {
            return true;
        }
        if (trySideButton(mouseX, mouseY, baseX, baseY + step, size, left)) {
            return true;
        }
        if (trySideButton(mouseX, mouseY, baseX + step, baseY + step, size, facing)) {
            return true;
        }
        if (trySideButton(mouseX, mouseY, baseX + step * 2, baseY + step, size, right)) {
            return true;
        }
        if (trySideButton(mouseX, mouseY, baseX + step, baseY + step * 2, size, Direction.DOWN)) {
            return true;
        }
        return trySideButton(mouseX, mouseY, baseX + step * 2, baseY + step * 2, size, back);
    }

    private boolean trySideButton(double mouseX, double mouseY, int x, int y, int size, Direction dir) {
        if (!isHovering(x, y, size, size, mouseX, mouseY)) {
            return false;
        }
        int buttonId = menu.getSideButtonId(currentType, dir);
        if (minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
        return true;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
    }
}
