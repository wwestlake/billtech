package com.billtech.config;

import com.billtech.BillTech;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BillTechConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "billtech.json";
    private static BillTechConfig INSTANCE = new BillTechConfig();

    public int configVersion = 1;
    public Energy energy = new Energy();
    public Furnace furnace = new Furnace();
    public Generator generator = new Generator();
    public CoalPyrolyzer coalPyrolyzer = new CoalPyrolyzer();
    public OilExtractor oilExtractor = new OilExtractor();
    public Distiller distiller = new Distiller();
    public PaperPress paperPress = new PaperPress();
    public MethaneCollector methaneCollector = new MethaneCollector();
    public MethaneGenerator methaneGenerator = new MethaneGenerator();
    public MethaneTank methaneTank = new MethaneTank();
    public Upgrades upgrades = new Upgrades();

    public static BillTechConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            saveDefault(configPath);
            return;
        }
        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            BillTechConfig loaded = GSON.fromJson(json, BillTechConfig.class);
            if (loaded != null) {
                loaded.applyDefaults();
                INSTANCE = loaded;
            }
        } catch (Exception e) {
            BillTech.LOGGER.warn("Failed to load billtech config, using defaults.", e);
        }
    }

    private static void saveDefault(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON.toJson(INSTANCE);
            Files.writeString(configPath, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            BillTech.LOGGER.warn("Failed to write default billtech config.", e);
        }
    }

    private void applyDefaults() {
        if (energy == null) {
            energy = new Energy();
        }
        if (furnace == null) {
            furnace = new Furnace();
        }
        if (generator == null) {
            generator = new Generator();
        }
        if (coalPyrolyzer == null) {
            coalPyrolyzer = new CoalPyrolyzer();
        }
        if (oilExtractor == null) {
            oilExtractor = new OilExtractor();
        }
        if (distiller == null) {
            distiller = new Distiller();
        }
        if (paperPress == null) {
            paperPress = new PaperPress();
        }
        if (methaneCollector == null) {
            methaneCollector = new MethaneCollector();
        }
        if (methaneGenerator == null) {
            methaneGenerator = new MethaneGenerator();
        }
        if (methaneTank == null) {
            methaneTank = new MethaneTank();
        }
        if (upgrades == null) {
            upgrades = new Upgrades();
        }
    }

    public static final class Energy {
        public long cableTransferRate = 2000;
    }

    public static final class Furnace {
        public long energyCapacity = 10000;
        public long energyPerTick = 10;
        public int baseInputStacks = 1;
        public int baseOutputStacks = 1;
    }

    public static final class Generator {
        public long energyCapacity = 10000;
        public long generationPerTick = 40;
        public int baseFuelSlots = 1;
    }

    public static final class CoalPyrolyzer {
        public long energyCapacity = 8000;
        public long energyPerTick = 10;
        public int ticksPerItem = 100;
        public long outputPerItem = 1000;
        public long outputBuffer = 8000;
    }

    public static final class OilExtractor {
        public long energyCapacity = 8000;
        public long energyPerTick = 10;
        public int ticksPerItem = 100;
        public long outputPerItem = 1000;
        public long outputBuffer = 8000;
    }

    public static final class Distiller {
        public long energyCapacity = 12000;
        public long energyPerTick = 20;
        public int ticksPerBatch = 200;
        public long inputPerBatch = 1000;
        public long outputLight = 700;
        public long outputHeavy = 250;
        public long outputSludge = 50;
        public long inputBuffer = 8000;
        public long outputBuffer = 8000;
    }

    public static final class PaperPress {
        public long energyCapacity = 4000;
        public long energyPerTick = 5;
        public int ticksPerItem = 100;
    }

    public static final class MethaneCollector {
        public long outputPerTick = 5;
        public long outputBuffer = 8000;
    }

    public static final class MethaneGenerator {
        public long energyCapacity = 8000;
        public long energyPerTick = 20;
        public long methanePerTick = 5;
        public long inputBuffer = 8000;
    }

    public static final class MethaneTank {
        public long capacity = 10000;
    }

    public static final class Upgrades {
        public double multiplier = 1.0;
        public int furnaceInputStacksPerUpgrade = 1;
        public int furnaceOutputStacksPerUpgrade = 1;
        public int generatorFuelSlotsPerUpgrade = 5;
        public long energyCapacityPerUpgrade = 5000;
        public double speedPerUpgrade = 0.25;
        public double powerDrawPerUpgrade = 0.25;
    }
}
