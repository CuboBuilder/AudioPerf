package com.audio.audioperf.network;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.audio.ClientAudioHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AudioStopPayload(int managerId, int codecId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AudioStopPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AudioPerf.MODID, "audio_stop"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, AudioStopPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeInt(p.managerId);
                buf.writeInt(p.codecId);
            },
            buf -> new AudioStopPayload(buf.readInt(), buf.readInt())
    );

    public static void handle(AudioStopPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> {
                ClientAudioHandler handler = ClientAudioHandler.get();
                if (handler != null) {
                    handler.handleAudioStop(payload.managerId, payload.codecId);
                }
            });
        }
    }
}
