package com.example.customdiary.event;

import com.example.customdiary.command.DiaryCommand;
import com.example.customdiary.config.DiaryConfig;
import com.example.customdiary.config.DiaryConfigManager;
import com.example.customdiary.data.DiarySavedData;
import com.example.customdiary.registry.ModItems;
import com.example.customdiary.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Iterator;
import java.util.List;

public class ServerEventHandler {

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DiarySavedData data = DiarySavedData.get(player.serverLevel());
            if (data.isGiveAllActive()) {
                DiaryCommand.ensureDiaryInInventory(player);
            }
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = event.getEntity().getItem();
            if (stack.is(ModItems.DIARY.get())) {
                DiarySavedData data = DiarySavedData.get(serverPlayer.serverLevel());
                if (data.isGiveAllActive()) {
                    event.setCanceled(true);
                    DiaryCommand.ensureDiaryInInventory(serverPlayer);

                    int dropCount = data.incrementDropCount(serverPlayer.getUUID());
                    handleDropPenalties(serverPlayer, dropCount, data);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DiarySavedData data = DiarySavedData.get(player.serverLevel());
            if (data.isGiveAllActive()) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                if (mainHand.is(ModItems.DIARY.get()) || offHand.is(ModItems.DIARY.get())) {
                    if (event.getTarget() instanceof ItemFrame || event.getTarget() instanceof ArmorStand) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            DiarySavedData data = DiarySavedData.get(level);

            if (data.isGiveAllActive()) {
                if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
                    AbstractContainerMenu menu = player.containerMenu;
                    int containerSlots = menu.slots.size() - 36;
                    for (int i = 0; i < containerSlots; i++) {
                        if (i >= 0 && i < menu.slots.size()) {
                            Slot slot = menu.getSlot(i);
                            if (slot.hasItem() && slot.getItem().is(ModItems.DIARY.get())) {
                                slot.set(ItemStack.EMPTY);
                                menu.broadcastChanges();
                            }
                        }
                    }
                }

                DiaryCommand.ensureDiaryInInventory(player);
            }

            if (data.getCursedFirePlayers().contains(player.getUUID())) {
                if (player.getHealth() > 1.0f) {
                    player.setRemainingFireTicks(100);
                } else {
                    player.clearFire();
                    data.getCursedFirePlayers().remove(player.getUUID());
                    data.setDirty();
                }
            }

            checkChapterProgression(player, level, data);
            processPendingTasks(level, data);
        }
    }

    private void handleDropPenalties(ServerPlayer player, int drops, DiarySavedData data) {
        if (drops == 5) {
            player.sendSystemMessage(Component.literal("Не стоит.").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
        } else if (drops == 10) {
            player.sendSystemMessage(Component.literal("Перестань.").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
        } else if (drops == 15) {
            player.sendSystemMessage(Component.literal("Слушай, если ты продолжишь тебе не поздоровится...").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW));
        } else if (drops == 20) {
            data.getCursedFirePlayers().add(player.getUUID());
            data.setDirty();
            player.setRemainingFireTicks(1200);
        } else if (drops == 30) {
            player.sendSystemMessage(Component.literal("Неужели ты не понимаешь?").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE));
            long currentTick = player.serverLevel().getGameTime();

            data.getPendingTasks().add(new DiarySavedData.PendingTask(player.getUUID(), currentTick + 60, 1));  // +3s
            data.getPendingTasks().add(new DiarySavedData.PendingTask(player.getUUID(), currentTick + 120, 2)); // +6s (Розовый текст + Звук)
            data.getPendingTasks().add(new DiarySavedData.PendingTask(player.getUUID(), currentTick + 260, 3)); // +13s (Молния)
            data.setDirty();
        }
    }

    private void checkChapterProgression(ServerPlayer player, ServerLevel level, DiarySavedData data) {
        int currentChapter = data.getPlayerChapter(player.getUUID());
        DiaryConfig config = DiaryConfigManager.getConfig();

        if (config != null && config.chapters != null) {
            for (DiaryConfig.Chapter chapter : config.chapters) {
                if (chapter.id == currentChapter + 1 && chapter.triggers != null) {
                    boolean biomeMatches = true;
                    if (chapter.triggers.biome != null && !chapter.triggers.biome.isEmpty()) {
                        ResourceLocation currentBiome = level.getBiome(player.blockPosition()).unwrapKey().get().location();
                        biomeMatches = currentBiome.toString().equals(chapter.triggers.biome);
                    }

                    boolean itemsMatch = true;
                    if (chapter.triggers.items != null && !chapter.triggers.items.isEmpty()) {
                        for (String itemId : chapter.triggers.items) {
                            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                            if (!player.getInventory().contains(new ItemStack(item))) {
                                itemsMatch = false;
                                break;
                            }
                        }
                    }

                    if (biomeMatches && itemsMatch) {
                        data.setPlayerChapter(player.getUUID(), chapter.id);
                        player.sendSystemMessage(Component.literal("Ваш дневник обновился: " + chapter.title).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                    }
                }
            }
        }
    }

    private void processPendingTasks(ServerLevel level, DiarySavedData data) {
        long currentTick = level.getGameTime();
        List<DiarySavedData.PendingTask> tasks = data.getPendingTasks();
        Iterator<DiarySavedData.PendingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            DiarySavedData.PendingTask task = iterator.next();
            if (currentTick >= task.executeTick) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(task.playerUUID);
                if (p != null && p.isAlive()) {
                    if (task.step == 1) {
                        p.sendSystemMessage(Component.literal("Это является оскорблением.").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
                    } else if (task.step == 2) {
                        p.sendSystemMessage(Component.literal("Впредь веди себя лучше.").withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE));

                        // Воспроизведение звука
                        level.playSound(
                                null,
                                p.getX(), p.getY(), p.getZ(),
                                ModSounds.PENALTY_SOUND.get(),
                                SoundSource.PLAYERS,
                                1.0f, 1.0f
                        );
                    } else if (task.step == 3) {
                        BlockPos pos = p.blockPosition();
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                        if (lightning != null) {
                            lightning.moveTo(pos.getX(), pos.getY(), pos.getZ());
                            level.addFreshEntity(lightning);
                        }
                        p.hurt(level.damageSources().lightningBolt(), 10000.0f);
                    }
                }
                iterator.remove();
                data.setDirty();
            }
        }
    }
}