package com.example.customdiary.command;

import com.example.customdiary.data.DiarySavedData;
import com.example.customdiary.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class DiaryCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("diary")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("giveall")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean active = BoolArgumentType.getBool(context, "active");
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        DiarySavedData data = DiarySavedData.get(player.serverLevel());
                        data.setGiveAllActive(active);

                        if (active) {
                            for (ServerPlayer p : player.serverLevel().getServer().getPlayerList().getPlayers()) {
                                ensureDiaryInInventory(p);
                            }
                            context.getSource().sendSuccess(() -> Component.literal("Режим выдачи дневников ВСЕМ включен."), true);
                        } else {
                            context.getSource().sendSuccess(() -> Component.literal("Режим выдачи дневников выключен."), true);
                        }
                        return 1;
                    })
                )
            )
        );
    }

    public static void ensureDiaryInInventory(ServerPlayer player) {
        int totalDiaries = 0;

        // 1. Проверяем предмет в курсоре мыши (когда игрок несёт его кликом)
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.is(ModItems.DIARY.get())) {
            totalDiaries += carried.getCount();
            if (totalDiaries > 1) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            } else if (carried.getCount() > 1) {
                carried.setCount(1);
            }
        }

        // 2. Проверяем все слоты инвентаря игрока
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.DIARY.get())) {
                totalDiaries += stack.getCount();
                if (totalDiaries > 1) {
                    player.getInventory().setItem(i, ItemStack.EMPTY); // Удаляем дубликат
                } else if (stack.getCount() > 1) {
                    stack.setCount(1); // Срезаем стак до 1 шт.
                }
            }
        }

        // 3. Если дневника нет ни в инвентаре, ни в курсоре — создаем 1 экземпляр
        if (totalDiaries == 0) {
            player.getInventory().add(new ItemStack(ModItems.DIARY.get()));
        }
    }
}