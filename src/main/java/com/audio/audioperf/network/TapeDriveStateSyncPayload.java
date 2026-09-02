package com.audio.audioperf.network;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.tile.TapeDriveState.State;
import com.audio.audioperf.tile.TileTapeDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TapeDriveStateSyncPayload(BlockPos pos, byte stateOrdinal) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TapeDriveStateSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AudioPerf.MODID, "tape_drive_state_sync"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, TapeDriveStateSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeByte(p.stateOrdinal);
            },
            buf -> new TapeDriveStateSyncPayload(buf.readBlockPos(), buf.readByte())
    );

    public static void handle(TapeDriveStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            if (level.getBlockEntity(payload.pos) instanceof TileTapeDrive td) {
                State[] states = State.VALUES;
                if (payload.stateOrdinal >= 0 && payload.stateOrdinal < states.length) {
                    td.onClientStateSync(states[payload.stateOrdinal]);
                }
            }
        });
    }
}