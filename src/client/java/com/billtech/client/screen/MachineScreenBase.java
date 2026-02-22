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

public abstract class MachineScreenBase<T extends MachineMenuBase> extends AbstractContainerScreen<T> {
    private static final int SLOT_BORDER = 0xFF141414;
    private static final int SLOT_OUTER = 0xFF3A3A3A;
    private static final int SLOT_INNER = 0xFF1E1E1E;
    private static final int SIDE_BORDER = 0xFF101010;
    private static final int SIDE_NONE = 0xFF3A3A3A;
    private static final int SIDE_INPUT = 0xFF3BD166;
    private static final int SIDE_OUTPUT = 0xFFD13B3B;
    private static final int SIDE_BOTH = 0xFF2E6FD1;

    private TransportType currentType;
    private Button tabMainButton;
    private Button tabStatusButton;
    private Button tabIoButton;
    private Button itemButton;
    private Button fluidButton;
    private Button energyButton;
    private Tab currentTab = Tab.MAIN;

    private enum Tab {
        MAIN,
        STATUS,
        IO
    }

    protected MachineScreenBase(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        currentType = defaultTransport();
        int tabY = topPos + 4;
        int tabWidth = 36;
        int tabHeight = 14;
        int tabX = leftPos + imageWidth - (tabWidth * 3) - 6;
        tabMainButton = Button.builder(Component.literal("Main"), button -> switchTab(Tab.MAIN))
                .pos(tabX, tabY)
                .size(tabWidth, tabHeight)
                .build();
        tabStatusButton = Button.builder(Component.literal("Status"), button -> switchTab(Tab.STATUS))
                .pos(tabX + tabWidth, tabY)
                .size(tabWidth, tabHeight)
                .build();
        tabIoButton = Button.builder(Component.literal("IO"), button -> switchTab(Tab.IO))
                .pos(tabX + tabWidth * 2, tabY)
                .size(tabWidth, tabHeight)
                .build();
        addRenderableWidget(tabMainButton);
        addRenderableWidget(tabStatusButton);
        addRenderableWidget(tabIoButton);

        int buttonY = topPos + 50;
        itemButton = buildTransportButton(TransportType.ITEM, leftPos + 8, buttonY);
        fluidButton = buildTransportButton(TransportType.FLUID, leftPos + 30, buttonY);
        energyButton = buildTransportButton(TransportType.ENERGY, leftPos + 52, buttonY);
        addRenderableWidget(itemButton);
        addRenderableWidget(fluidButton);
        addRenderableWidget(energyButton);
        updateTabButtons();
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

    private void switchTab(Tab tab) {
        currentTab = tab;
        updateTabButtons();
        updateTransportButtons();
    }

    private void updateTabButtons() {
        tabMainButton.active = currentTab != Tab.MAIN;
        tabStatusButton.active = currentTab != Tab.STATUS;
        tabIoButton.active = currentTab != Tab.IO;
    }

    private void updateTransportButtons() {
        boolean show = currentTab == Tab.IO;
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

        renderTabs(graphics);

        if (currentTab == Tab.MAIN) {
            for (Slot slot : menu.slots) {
                int x = leftPos + slot.x;
                int y = topPos + slot.y;
                graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
                graphics.fill(x, y, x + 16, y + 16, SLOT_OUTER);
                graphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_INNER);
            }
        } else {
            graphics.fill(leftPos + 6, topPos + 22, leftPos + imageWidth - 6, topPos + imageHeight - 8, 0xFF242424);
        }

        if (currentTab == Tab.IO) {
            renderSideConfig(graphics);
        }
    }

    private void renderTabs(GuiGraphics graphics) {
        int tabY = topPos + 4;
        int tabWidth = 36;
        int tabHeight = 14;
        int tabX = leftPos + imageWidth - (tabWidth * 3) - 6;
        int border = 0xFF0E0E0E;
        int inactive = 0xFF2E2E2E;
        int active = 0xFF3C3C3C;
        renderTab(graphics, tabX, tabY, tabWidth, tabHeight, currentTab == Tab.MAIN, border, inactive, active);
        renderTab(graphics, tabX + tabWidth, tabY, tabWidth, tabHeight, currentTab == Tab.STATUS, border, inactive, active);
        renderTab(graphics, tabX + tabWidth * 2, tabY, tabWidth, tabHeight, currentTab == Tab.IO, border, inactive, active);
    }

    private void renderTab(GuiGraphics graphics, int x, int y, int width, int height, boolean isActive,
                           int border, int inactive, int active) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, border);
        graphics.fill(x, y, x + width, y + height, isActive ? active : inactive);
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
        if (currentTab == Tab.IO && handleSideClick(mouseX, mouseY)) {
            return true;
        }
        if (currentTab != Tab.MAIN && isHoveringAnySlot(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSideClick(double mouseX, double mouseY) {
        int baseX = imageWidth - 70;
        int baseY = 42;
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
        if (!inBounds(x, y, size, size, mouseX, mouseY)) {
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
        graphics.drawString(font, title, 8, 22, 0xFFFFFF, false);
        if (currentTab == Tab.STATUS && useDefaultStatusLines()) {
            renderStatusLines(graphics);
        }
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (currentTab != Tab.MAIN) {
            return;
        }
        super.renderSlot(graphics, slot);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (currentTab != Tab.MAIN) {
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    protected boolean useDefaultStatusLines() {
        return true;
    }

    protected boolean isStatusTabActive() {
        return currentTab == Tab.STATUS;
    }

    private void renderStatusLines(GuiGraphics graphics) {
        if (!menu.hasStatus()) {
            return;
        }
        int x = 8;
        int y = 34;
        int color = 0xD0D0D0;
        if (menu.getStatusEnergyCapacity() > 0) {
            graphics.drawString(font, "Energy: " + menu.getStatusEnergyStored() + " / " + menu.getStatusEnergyCapacity(), x, y, color, false);
            y += 10;
        }
        if (menu.getStatusFluidInCapacity() > 0) {
            graphics.drawString(font, "Fluid In: " + menu.getStatusFluidInStored() + " / " + menu.getStatusFluidInCapacity(), x, y, color, false);
            y += 10;
        }
        if (menu.getStatusFluidOutCapacity() > 0) {
            graphics.drawString(font, "Fluid Out: " + menu.getStatusFluidOutStored() + " / " + menu.getStatusFluidOutCapacity(), x, y, color, false);
        }
    }

    private boolean isHoveringAnySlot(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (inBounds(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private boolean inBounds(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= (double) (leftPos + x)
                && mouseX < (double) (leftPos + x + width)
                && mouseY >= (double) (topPos + y)
                && mouseY < (double) (topPos + y + height);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (currentTab != Tab.MAIN) {
            return false;
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }
}
