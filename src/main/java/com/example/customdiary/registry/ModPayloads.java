package com.example.customdiary.registry;

import com.example.customdiary.network.SyncDiaryPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncDiaryPayload.TYPE,
                SyncDiaryPayload.STREAM_CODEC,
                SyncDiaryPayload::handle
        );
    }
}