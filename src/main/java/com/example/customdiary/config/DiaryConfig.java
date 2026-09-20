package com.example.customdiary.config;

import java.util.ArrayList;
import java.util.List;

public class DiaryConfig {
    public List<Chapter> chapters = new ArrayList<>();

    public static class Chapter {
        public int id;
        public String title = "";
        public Triggers triggers;
        public List<Page> pages = new ArrayList<>();
    }

    public static class Triggers {
        public String biome;
        public List<String> items;
    }

    public static class Page {
        public String text = "";
        public String image; // ResourceLocation format e.g. "minecraft:textures/item/diamond.png"
    }
}