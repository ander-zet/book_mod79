package com.example.customdiary.item;

import com.example.customdiary.config.DiaryConfig;
import com.example.customdiary.config.DiaryConfigManager;
import com.example.customdiary.data.DiarySavedData;
import com.example.customdiary.network.SyncDiaryPayload;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DiaryItem extends Item {
    private static final Gson GSON = new Gson();

    public DiaryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = serverPlayer.serverLevel();
            DiarySavedData data = DiarySavedData.get(serverLevel);
            int currentChapter = data.getPlayerChapter(player.getUUID());

            String jsonConfig = GSON.toJson(DiaryConfigManager.getConfig());
            SyncDiaryPayload payload = new SyncDiaryPayload(currentChapter, jsonConfig);

            serverPlayer.connection.send(payload);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}