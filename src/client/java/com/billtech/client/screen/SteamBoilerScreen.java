package com.billtech.client.screen;

import com.billtech.menu.SteamBoilerMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SteamBoilerScreen extends MachineScreenBase<SteamBoilerMenu> {
    private enum ComponentView {
        BOILER,
        ENGINE,
        GENERATOR
    }

    private ComponentView componentView = ComponentView.BOILER;
    private Button boilerButton;
    private Button engineButton;
    private Button generatorButton;

    public SteamBoilerScreen(SteamBoilerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + 24;
        boilerButton = Button.builder(Component.literal("Boiler"), b -> componentView = ComponentView.BOILER)
                .pos(leftPos + 8, y).size(40, 14).build();
        engineButton = Button.builder(Component.literal("Engine"), b -> componentView = ComponentView.ENGINE)
                .pos(leftPos + 50, y).size(40, 14).build();
        generatorButton = Button.builder(Component.literal("Gen"), b -> componentView = ComponentView.GENERATOR)
                .pos(leftPos + 92, y).size(40, 14).build();
        addRenderableWidget(boilerButton);
        addRenderableWidget(engineButton);
        addRenderableWidget(generatorButton);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (boilerButton != null) {
            boolean status = isStatusTabActive();
            boilerButton.visible = status;
            engineButton.visible = status;
            generatorButton.visible = status;
            boilerButton.active = status && componentView != ComponentView.BOILER;
            engineButton.active = status && componentView != ComponentView.ENGINE;
            generatorButton.active = status && componentView != ComponentView.GENERATOR;
        }
        if (!isStatusTabActive()) {
            return;
        }
        switch (componentView) {
            case BOILER -> {
                graphics.drawString(font, "Water: " + menu.getWaterAmount() + " / " + menu.getWaterBuffer() + " mB", 8, 42, 0xCCCCCC, false);
                graphics.drawString(font, "Fuel: " + menu.getFuelAmount() + " / " + menu.getFuelBuffer() + " mB", 8, 54, 0xCCCCCC, false);
                graphics.drawString(font, "Steam: " + menu.getSteamAmount() + " / " + menu.getSteamBuffer() + " mB", 8, 66, 0xCCCCCC, false);
                graphics.drawString(font, "Rate: " + menu.getSteamPerTick() + " mB/t", 8, 78, 0xCCCCCC, false);
                graphics.drawString(font, "Burn: " + menu.getBurnTime() + " / " + menu.getBurnTimeTotal(), 8, 90, 0xCCCCCC, false);
                graphics.drawString(font, "Feed Valve: " + (menu.isTurbineFeedEnabled() ? "Open" : "Closed"), 8, 102, 0xCCCCCC, false);
            }
            case ENGINE -> {
                graphics.drawString(font, "Steam In: " + menu.getEngineSteamAmount() + " / " + menu.getEngineSteamCapacity() + " mB", 8, 42, 0xCCCCCC, false);
                graphics.drawString(font, "Role: Steam Throughput Stage", 8, 54, 0xCCCCCC, false);
                graphics.drawString(font, "Controller: Boiler", 8, 66, 0xCCCCCC, false);
            }
            case GENERATOR -> {
                graphics.drawString(font, "Steam In: " + menu.getGeneratorSteamAmount() + " / " + menu.getGeneratorSteamCapacity() + " mB", 8, 42, 0xCCCCCC, false);
                graphics.drawString(font, "Energy: " + menu.getGeneratorEnergyAmount() + " / " + menu.getGeneratorEnergyCapacity(), 8, 54, 0xCCCCCC, false);
                graphics.drawString(font, "Role: Steam -> Power", 8, 66, 0xCCCCCC, false);
            }
        }
        graphics.drawString(font, "Structure: " + (menu.isMultiblockComplete() ? "Assembled" : "Missing Engine/Generator"), 8, 114, 0xCCCCCC, false);
    }

    @Override
    protected boolean useDefaultStatusLines() {
        return false;
    }
}
