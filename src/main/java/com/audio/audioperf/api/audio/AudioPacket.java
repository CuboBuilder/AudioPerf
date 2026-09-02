package com.audio.audioperf.api.audio;

import com.audio.audioperf.network.AudioDataPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AudioPacket {
    private static int _idGen;
    private static synchronized int getNewId() {
        return _idGen++;
    }

    public final IAudioSource source;
    public final int id;
    public final byte volume;

    private final Set<IAudioReceiver> receivers = new HashSet<>();

    public AudioPacket(IAudioSource source, byte volume) {
        this.id = getNewId();
        this.source = source;
        this.volume = volume;
    }

    public Collection<IAudioReceiver> getReceivers() {
        return Collections.unmodifiableSet(receivers);
    }

    public void addReceiver(IAudioReceiver receiver) {
        if (receiver.getSoundWorld() != null) {
            receivers.add(receiver);
        }
    }

    /**
     * Writes the packet-type-specific payload data into the given buffer.
     */
    protected abstract void writeData(ByteBuf buffer);

    protected boolean canHearReceiver(ServerPlayer player, IAudioReceiver receiver) {
        if (!(receiver.getSoundWorld() instanceof ServerLevel receiverLevel)
                || player.level() != receiverLevel) {
            return false;
        }

        int mdSq = receiver.getSoundDistance() * receiver.getSoundDistance();
        final Vec3 pos = receiver.getSoundPos();
        double distSq = (pos.x - player.getX()) * (pos.x - player.getX());
        distSq += (pos.y - player.getY()) * (pos.y - player.getY());
        distSq += (pos.z - player.getZ()) * (pos.z - player.getZ());
        return distSq <= mdSq;
    }

    /**
     * Sends the audio packet to all players that are within hearing range of
     * at least one of the collected receivers. Server side only.
     */
    public final void sendPacket() {
        net.minecraft.server.MinecraftServer server = com.audio.audioperf.AudioPerf.getServer();
        if (server == null) {
            return;
        }

        ByteBuf data = Unpooled.buffer();
        writeData(data);
        byte[] payloadData = new byte[data.readableBytes()];
        data.readBytes(payloadData);
        data.release();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == null || player.level() == null) {
                continue;
            }

            List<AudioDataPayload.Receiver> receiversLocal = new ArrayList<>();
            for (IAudioReceiver receiver : receivers) {
                if (canHearReceiver(player, receiver)) {
                    final Vec3 pos = receiver.getSoundPos();
                    receiversLocal.add(new AudioDataPayload.Receiver(
                            receiver.getSoundWorld().dimension().location().toString(),
                            (float) pos.x, (float) pos.y, (float) pos.z,
                            (short) receiver.getSoundDistance(), volume, receiver.getID()));
                }
            }

            if (!receiversLocal.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new AudioDataPayload(
                        (short) AudioPacketRegistry.INSTANCE.getId(this.getClass()),
                        id,
                        source.getSourceId(),
                        payloadData,
                        receiversLocal));
            }
        }
    }
}
