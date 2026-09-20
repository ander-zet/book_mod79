package com.example.customdiary.network;

import com.example.customdiary.CustomDiaryMod;
import com.example.customdiary.client.DiaryScreen;
import com.example.customdiary.config.DiaryConfig;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncDiaryPayload(int currentChapter, String configJson) implements CustomPacketPayload {
    public static final Type<SyncDiaryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CustomDiaryMod.MOD_ID, "sync_diary"));

    public static final StreamCodec<FriendlyByteBuf, SyncDiaryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncDiaryPayload::currentChapter,
            ByteBufCodecs.STRING_UTF8, SyncDiaryPayload::configJson,
            SyncDiaryPayload::new
    );

    @Override
    public Type<SyncDiaryPayload> type() {
        return TYPE;
    }

    public static void handle(SyncDiaryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Gson gson = new Gson();
            DiaryConfig config = gson.fromJson(payload.configJson(), DiaryConfig.class);
            Minecraft.getInstance().setScreen(new DiaryScreen(payload.currentChapter(), config));
        });
    }
}