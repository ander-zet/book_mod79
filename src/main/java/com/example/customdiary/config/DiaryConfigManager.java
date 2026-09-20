package com.example.customdiary.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class DiaryConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DiaryConfig config = new DiaryConfig();

    public static void loadConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("custom_diary.json");
        File file = configPath.toFile();

        if (!file.exists()) {
            createDefaultConfig(file);
        }

        try (FileReader reader = new FileReader(file)) {
            config = GSON.fromJson(reader, DiaryConfig.class);
            if (config == null) config = new DiaryConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig(File file) {
        DiaryConfig defaultConfig = new DiaryConfig();

        DiaryConfig.Chapter ch0 = new DiaryConfig.Chapter();
        ch0.id = 0;
        ch0.title = "Глава 0: Пробуждение";
        DiaryConfig.Page p0 = new DiaryConfig.Page();
        p0.text = "Вы очнулись в этом мире с древней книгой в руках. Она хранит ваши секреты.";
        p0.image = "minecraft:textures/gui/sprites/icon/checkmark.png";
        ch0.pages.add(p0);

        DiaryConfig.Chapter ch1 = new DiaryConfig.Chapter();
        ch1.id = 1;
        ch1.title = "Глава 1: Тьма и Алмазы";
        ch1.triggers = new DiaryConfig.Triggers();
        ch1.triggers.biome = "minecraft:deep_dark";
        ch1.triggers.items = List.of("minecraft:diamond");
        DiaryConfig.Page p1 = new DiaryConfig.Page();
        p1.text = "Вы спустились в сияющие и тёмные глубины с редким сокровищем.";
        p1.image = "minecraft:textures/item/diamond.png";
        ch1.pages.add(p1);

        defaultConfig.chapters.add(ch0);
        defaultConfig.chapters.add(ch1);

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(defaultConfig, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DiaryConfig getConfig() {
        return config;
    }
}