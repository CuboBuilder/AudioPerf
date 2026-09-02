package com.audio.audioperf.network;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.audio.ClientAudioHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record AudioDataPayload(
        short packetTypeId,
        int packetId,
        int sourceId,
        byte[] data,
        List<Receiver> receivers
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AudioDataPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AudioPerf.MODID, "audio_data"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Receiver(
            String dimension,
            float x, float y, float z,
            int distance,
            byte volume,
            String deviceId
    ) {
        public static final StreamCodec<FriendlyByteBuf, Receiver> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    buf.writeUtf(r.dimension);
                    buf.writeFloat(r.x);
                    buf.writeFloat(r.y);
                    buf.writeFloat(r.z);
                    buf.writeShort(r.distance);
                    buf.writeByte(r.volume);
                    buf.writeUtf(r.deviceId);
                },
                buf -> new Receiver(
                        buf.readUtf(),
                        buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readUnsignedShort(),
                        buf.readByte(),
                        buf.readUtf()
                )
        );
    }

    public static final StreamCodec<FriendlyByteBuf, AudioDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeShort(p.packetTypeId);
                buf.writeInt(p.packetId);
                buf.writeInt(p.sourceId);
                buf.writeShort(p.data.length);
                buf.writeBytes(p.data);
                buf.writeShort(p.receivers.size());
                for (Receiver r : p.receivers) {
                    Receiver.STREAM_CODEC.encode(buf, r);
                }
            },
            buf -> {
                short packetTypeId = buf.readShort();
                int packetId = buf.readInt();
                int sourceId = buf.readInt();
                short dataLen = buf.readShort();
                byte[] data = new byte[dataLen];
                buf.readBytes(data);
                short receiverCount = buf.readShort();
                List<Receiver> receivers = new ArrayList<>(receiverCount);
                for (int i = 0; i < receiverCount; i++) {
                    receivers.add(Receiver.STREAM_CODEC.decode(buf));
                }
                return new AudioDataPayload(packetTypeId, packetId, sourceId, data, receivers);
            }
    );

    public static void handle(AudioDataPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> {
                ClientAudioHandler handler = ClientAudioHandler.get();
                if (handler == null) return;
                handler.handlePayload(payload);
            });
        }
    }
}
