package com.example.tornadomod;

import net.minecraftforge.common.ForgeConfigSpec;

public class TornadoConfig {

    // ── Common (server) ──────────────────────────────────────────
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue  NATURAL_SPAWN;
    public static final ForgeConfigSpec.DoubleValue   TORNADO_FREQ;
    public static final ForgeConfigSpec.DoubleValue   HURRICANE_FREQ;
    public static final ForgeConfigSpec.IntValue      MAX_STORMS;
    public static final ForgeConfigSpec.BooleanValue  DESTROY_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue  PROTECT_BUILDINGS;
    public static final ForgeConfigSpec.IntValue      MAX_DEBRIS;

    // ── Client ───────────────────────────────────────────────────
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.IntValue      PARTICLE_DENSITY;
    public static final ForgeConfigSpec.IntValue      CLOUD_QUALITY;
    public static final ForgeConfigSpec.BooleanValue  RAIN_FOG;
    public static final ForgeConfigSpec.EnumValue<CloudMode> CLOUD_MODE;
    public static final ForgeConfigSpec.BooleanValue  REPLACE_VANILLA_CLOUDS;

    // New: cloud render distance (chunks)
    public static final ForgeConfigSpec.IntValue      CLOUD_DISTANCE;

    // New: cloud type toggles
    public static final ForgeConfigSpec.BooleanValue  CUMULUS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  CUMULONIMBUS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  STRATUS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  CIRRUS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  ALTOCUMULUS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  WALL_CLOUDS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  SPIRAL_BANDS_ENABLED;

    // New: fog settings
    public static final ForgeConfigSpec.BooleanValue  GROUND_FOG_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  MOUNTAIN_FOG_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  STORM_FOG_ENABLED;
    public static final ForgeConfigSpec.BooleanValue  ATMOSPHERIC_HAZE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue   FOG_DENSITY;
    public static final ForgeConfigSpec.DoubleValue   FOG_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue   FOG_INTENSITY;

    // New: quality preset
    public static final ForgeConfigSpec.EnumValue<QualityPreset> QUALITY_PRESET;

    public enum CloudMode { NONE, FLAT, VOLUMETRIC }
    public enum QualityPreset { LOW, MEDIUM, HIGH, ULTRA }

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("storms");
        NATURAL_SPAWN      = common.define("naturalSpawn", true);
        TORNADO_FREQ       = common.defineInRange("tornadoFrequency",  1.0, 0.0, 20.0);
        HURRICANE_FREQ     = common.defineInRange("hurricaneFrequency",1.0, 0.0, 20.0);
        MAX_STORMS         = common.defineInRange("maxStorms",         4,   1,   20);
        common.pop();

        common.push("damage");
        DESTROY_BLOCKS     = common.define("destroyBlocks",    true);
        PROTECT_BUILDINGS  = common.define("protectBuildings", true);
        MAX_DEBRIS         = common.defineInRange("maxDebris", 40, 0, 200);
        common.pop();

        COMMON_SPEC = common.build();

        // ── client ──
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("clouds");
        PARTICLE_DENSITY       = client.defineInRange("particleDensity",  2, 0, 5);
        CLOUD_QUALITY          = client.defineInRange("cloudQuality",      2, 0, 3);
        RAIN_FOG               = client.define("rainFog",                  true);
        CLOUD_MODE             = client.defineEnum("cloudMode",            CloudMode.VOLUMETRIC);
        REPLACE_VANILLA_CLOUDS = client.define("replaceVanillaClouds",     true);
        CLOUD_DISTANCE         = client.defineInRange("cloudDistance",     64, 8, 192);
        QUALITY_PRESET         = client.defineEnum("qualityPreset",        QualityPreset.MEDIUM);

        client.push("cloudTypes");
        CUMULUS_ENABLED        = client.define("cumulus",       true);
        CUMULONIMBUS_ENABLED   = client.define("cumulonimbus",  true);
        STRATUS_ENABLED        = client.define("stratus",       true);
        CIRRUS_ENABLED         = client.define("cirrus",        true);
        ALTOCUMULUS_ENABLED    = client.define("altocumulus",   true);
        WALL_CLOUDS_ENABLED    = client.define("wallClouds",    true);
        SPIRAL_BANDS_ENABLED   = client.define("spiralBands",   true);
        client.pop();

        client.push("fog");
        GROUND_FOG_ENABLED     = client.define("groundFog",       true);
        MOUNTAIN_FOG_ENABLED   = client.define("mountainFog",     true);
        STORM_FOG_ENABLED      = client.define("stormFog",        true);
        ATMOSPHERIC_HAZE_ENABLED = client.define("atmosphericHaze", true);
        FOG_DENSITY            = client.defineInRange("fogDensity",   0.5, 0.0, 1.0);
        FOG_HEIGHT             = client.defineInRange("fogHeight",    60.0, 0.0, 256.0);
        FOG_INTENSITY          = client.defineInRange("fogIntensity", 0.7, 0.0, 1.0);
        client.pop();

        client.pop(); // clouds
        CLIENT_SPEC = client.build();
    }
}
