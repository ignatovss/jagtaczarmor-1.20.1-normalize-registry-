//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.mojang.logging.LogUtils;
import java.io.File;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public class ArmorConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File CONFIG_FILE;
    public static ConfigData DATA;

    public ArmorConfig() {
    }

    public static void load() {
        Config.setInsertionOrderPreserved(true);
        DATA = new ConfigData();
        CommentedFileConfig config = (CommentedFileConfig)CommentedFileConfig.builder(CONFIG_FILE).sync().autosave().writingMode(WritingMode.REPLACE).preserveInsertionOrder().build();
        if (!CONFIG_FILE.exists()) {
            save(config);
        } else {
            try {
                config.load();
                DATA.impact_tilt_multiplier = getDouble(config, "impact_tilt_multiplier", DATA.impact_tilt_multiplier);
                DATA.impact_tilt_duration = getInt(config, "impact_tilt_duration", DATA.impact_tilt_duration);
                DATA.impact_tilt_frequency = getDouble(config, "impact_tilt_frequency", DATA.impact_tilt_frequency);
                DATA.impact_tilt_decay_exponent = getDouble(config, "impact_tilt_decay_exponent", DATA.impact_tilt_decay_exponent);
                DATA.penetrant_tilt_multiplier = getDouble(config, "penetrant_tilt_multiplier", DATA.penetrant_tilt_multiplier);
                DATA.penetrant_tilt_duration = getInt(config, "penetrant_tilt_duration", DATA.penetrant_tilt_duration);
                DATA.penetrant_tilt_frequency = getDouble(config, "penetrant_tilt_frequency", DATA.penetrant_tilt_frequency);
                DATA.penetrant_tilt_decay_exponent = getDouble(config, "penetrant_tilt_decay_exponent", DATA.penetrant_tilt_decay_exponent);
                DATA.hurt_tilt_multiplier = getDouble(config, "hurt_tilt_multiplier", DATA.hurt_tilt_multiplier);
                DATA.hurt_tilt_duration = getInt(config, "hurt_tilt_duration", DATA.hurt_tilt_duration);
                DATA.hurt_tilt_frequency = getDouble(config, "hurt_tilt_frequency", DATA.hurt_tilt_frequency);
                DATA.hurt_tilt_decay_exponent = getDouble(config, "hurt_tilt_decay_exponent", DATA.hurt_tilt_decay_exponent);
                DATA.enable_no_armor_hit_shaking = getBoolean(config, "enable_no_armor_hit_shaking", DATA.enable_no_armor_hit_shaking);
                DATA.enable_impact_shaking = getBoolean(config, "enable_impact_shaking", DATA.enable_impact_shaking);
                DATA.enable_penetrant_shaking = getBoolean(config, "enable_penetrant_shaking", DATA.enable_penetrant_shaking);
                DATA.enable_impact_sound = getBoolean(config, "enable_impact_sound", DATA.enable_impact_sound);
                DATA.enable_hurt_sound = getBoolean(config, "enable_hurt_sound", DATA.enable_hurt_sound);
                DATA.enable_vignette = getBoolean(config, "enable_vignette", DATA.enable_vignette);
                DATA.enable_blood_particles = getBoolean(config, "enable_blood_particles", DATA.enable_blood_particles);
                DATA.vignette_intensity = getDouble(config, "vignette_intensity", DATA.vignette_intensity);
                DATA.vignette_duration = getInt(config, "vignette_duration", DATA.vignette_duration);
                DATA.overwrite_default_pack = getBoolean(config, "overwrite_default_pack", DATA.overwrite_default_pack);
                DATA.default_shake_helmet = getDouble(config, "default_shake_helmet", DATA.default_shake_helmet);
                DATA.default_shake_chestplate = getDouble(config, "default_shake_chestplate", DATA.default_shake_chestplate);
                DATA.default_shake_leggings = getDouble(config, "default_shake_leggings", DATA.default_shake_leggings);
                DATA.default_shake_boots = getDouble(config, "default_shake_boots", DATA.default_shake_boots);
                save(config);
            } catch (Exception e) {
                LOGGER.error("Failed to load jagtaczarmor config", e);
            }
        }

    }

    private static double getDouble(CommentedFileConfig config, String path, double defaultValue) {
        Object val = config.get(path);
        if (val instanceof Number num) {
            return num.doubleValue();
        } else {
            return defaultValue;
        }
    }

    private static int getInt(CommentedFileConfig config, String path, int defaultValue) {
        Object val = config.get(path);
        if (val instanceof Number num) {
            return num.intValue();
        } else {
            return defaultValue;
        }
    }

    private static boolean getBoolean(CommentedFileConfig config, String path, boolean defaultValue) {
        Object val = config.get(path);
        if (val instanceof Boolean b) {
            return b;
        } else {
            return defaultValue;
        }
    }

    public static void save() {
        Config.setInsertionOrderPreserved(true);
        CommentedFileConfig config = (CommentedFileConfig)CommentedFileConfig.builder(CONFIG_FILE).sync().autosave().writingMode(WritingMode.REPLACE).preserveInsertionOrder().build();
        save(config);
    }

    private static void save(CommentedFileConfig config) {
        try {
            config.clear();
            config.setComment("impact_tilt_multiplier", "--- Armor Impact Camera Effects (When armor is hit) ---");
            config.set("impact_tilt_multiplier", DATA.impact_tilt_multiplier);
            config.set("impact_tilt_duration", DATA.impact_tilt_duration);
            config.set("impact_tilt_frequency", DATA.impact_tilt_frequency);
            config.set("impact_tilt_decay_exponent", DATA.impact_tilt_decay_exponent);
            config.setComment("penetrant_tilt_multiplier", "\n--- Armor Penetrant Camera Effects (When armor is hit AND penetrated) ---");
            config.set("penetrant_tilt_multiplier", DATA.penetrant_tilt_multiplier);
            config.set("penetrant_tilt_duration", DATA.penetrant_tilt_duration);
            config.set("penetrant_tilt_frequency", DATA.penetrant_tilt_frequency);
            config.set("penetrant_tilt_decay_exponent", DATA.penetrant_tilt_decay_exponent);
            config.setComment("hurt_tilt_multiplier", "\n--- No Armor Hurt Camera Effects (When hit without armor at location) ---");
            config.set("hurt_tilt_multiplier", DATA.hurt_tilt_multiplier);
            config.set("hurt_tilt_duration", DATA.hurt_tilt_duration);
            config.set("hurt_tilt_frequency", DATA.hurt_tilt_frequency);
            config.set("hurt_tilt_decay_exponent", DATA.hurt_tilt_decay_exponent);
            config.setComment("enable_no_armor_hit_shaking", "\nWhen true, custom camera shake plays even when the hit location has no armor.\nWhen false, vanilla tilt is used instead if no armor is at the hit location.");
            config.set("enable_no_armor_hit_shaking", DATA.enable_no_armor_hit_shaking);
            config.setComment("enable_impact_shaking", "\nToggle settings for visuals and audio");
            config.set("enable_impact_shaking", DATA.enable_impact_shaking);
            config.set("enable_penetrant_shaking", DATA.enable_penetrant_shaking);
            config.set("enable_impact_sound", DATA.enable_impact_sound);
            config.set("enable_hurt_sound", DATA.enable_hurt_sound);
            config.set("enable_vignette", DATA.enable_vignette);
            config.set("enable_blood_particles", DATA.enable_blood_particles);
            config.set("vignette_intensity", DATA.vignette_intensity);
            config.set("vignette_duration", DATA.vignette_duration);
            config.setComment("overwrite_default_pack", "\nIf true, the mod will overwrite the jag_default_armor pack every time the game runs.\nIf false, it will keep any modifications made to that pack.");
            config.set("overwrite_default_pack", DATA.overwrite_default_pack);
            config.setComment("default_shake_helmet", "\nDefault camera_shake_multiplier per slot.");
            config.set("default_shake_helmet", DATA.default_shake_helmet);
            config.set("default_shake_chestplate", DATA.default_shake_chestplate);
            config.set("default_shake_leggings", DATA.default_shake_leggings);
            config.set("default_shake_boots", DATA.default_shake_boots);
            config.save();
        } catch (Exception e) {
            LOGGER.error("Failed to save jagtaczarmor config", e);
        }

    }

    static {
        CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "jagtaczarmor.toml");
    }

    public static class ConfigData {
        public double impact_tilt_multiplier = (double)1.0F;
        public int impact_tilt_duration = 12;
        public double impact_tilt_frequency = (double)1.0F;
        public double impact_tilt_decay_exponent = 1.2;
        public double penetrant_tilt_multiplier = (double)1.0F;
        public int penetrant_tilt_duration = 8;
        public double penetrant_tilt_frequency = (double)1.5F;
        public double penetrant_tilt_decay_exponent = 1.8;
        public double hurt_tilt_multiplier = 1.6;
        public int hurt_tilt_duration = 6;
        public double hurt_tilt_frequency = (double)2.0F;
        public double hurt_tilt_decay_exponent = 1.8;
        public boolean enable_no_armor_hit_shaking = true;
        public boolean enable_impact_shaking = true;
        public boolean enable_penetrant_shaking = true;
        public boolean enable_impact_sound = true;
        public boolean enable_hurt_sound = true;
        public boolean enable_vignette = true;
        public boolean enable_blood_particles = false;
        public double vignette_intensity = (double)1.0F;
        public int vignette_duration = 10;
        public boolean overwrite_default_pack = true;
        public double default_shake_helmet = 1.2;
        public double default_shake_chestplate = (double)1.0F;
        public double default_shake_leggings = 0.8;
        public double default_shake_boots = 0.6;

        public ConfigData() {
        }
    }
}
