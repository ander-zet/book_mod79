package com.example.customdiary;

import com.example.customdiary.config.DiaryConfigManager;
import com.example.customdiary.event.ServerEventHandler;
import com.example.customdiary.registry.ModItems;
import com.example.customdiary.registry.ModPayloads;
import com.example.customdiary.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(CustomDiaryMod.MOD_ID)
public class CustomDiaryMod {
    public static final String MOD_ID = "customdiary";
    public static final String MODID = MOD_ID; // Константа для совместимости со старыми вызовами
    public static final Logger LOGGER = LogUtils.getLogger();

    public CustomDiaryMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);
        
        // Исправление 1: регистрация пакетов через слушатель события
        modEventBus.addListener(ModPayloads::register);

        NeoForge.EVENT_BUS.register(new ServerEventHandler());
        
        // Исправление 2: регистрация команд через RegisterCommandsEvent
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> 
            com.example.customdiary.command.DiaryCommand.register(event.getDispatcher())
        );

        DiaryConfigManager.loadConfig();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("CustomDiaryMod common setup completed.");
    }
}