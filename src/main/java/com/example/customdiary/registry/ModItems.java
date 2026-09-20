package com.example.customdiary.registry;

import com.example.customdiary.CustomDiaryMod;
import com.example.customdiary.item.DiaryItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CustomDiaryMod.MOD_ID);

    public static final DeferredItem<Item> DIARY = ITEMS.register("diary",
            () -> new DiaryItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}