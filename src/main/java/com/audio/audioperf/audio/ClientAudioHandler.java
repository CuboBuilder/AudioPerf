package com.audio.audioperf.audio;

import com.audio.audioperf.network.AudioDataPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientAudioHandler {
    private static ClientAudioHandler instance;

    public static ClientAudioHandler get() {
        return instance;
    }

    public static void create() {
        instance = new ClientAudioHandler();
    }

    private final DFPWMPlaybackManager playbackManager = new DFPWMPlaybackManager(true);

    public DFPWMPlaybackManager getPlaybackManager() {
        return playbackManager;
    }

    public void handleAudioData(int packetTypeId, int packetId, int codecId, byte[] data) {
        try {
            ByteBuf buf = Unpooled.wrappedBuffer(data);
            int sampleRate = buf.readInt();
            short packetSize = buf.readShort();
            byte[] rawData = new byte[packetSize];
            buf.readBytes(rawData);
            buf.release();

            byte[] audio = new byte[packetSize * 8];
            StreamingAudioPlayer codec = playbackManager.getPlayer(codecId);
            if (codec == null) {
                codec = playbackManager.createPlayer(codecId);
            }
            codec.decompress(audio, rawData, 0, 0, packetSize);
            for (int i = 0; i < (packetSize * 8); i++) {
                audio[i] = (byte) (((int) audio[i] & 0xFF) ^ 0x80);
            }
            codec.setSampleRate(sampleRate);
            codec.push(audio);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handlePlayData(int packetId, int codecId, float x, float y, float z, int distance, byte volume, String deviceId) {
        StreamingAudioPlayer codec = playbackManager.getPlayer(codecId);
        if (codec == null) return;
        codec.setHearing((float) distance, volume / 127.0F);
        codec.play("computronics:dfpwm" + codecId + (deviceId.isEmpty() ? "" : "-" + deviceId), x, y, z);
    }

    public void handleAudioStop(int managerId, int codecId) {
        playbackManager.removePlayer(codecId);
    }

    public void handlePayload(AudioDataPayload payload) {
        handleAudioData(payload.packetTypeId(), payload.packetId(), payload.sourceId(), payload.data());
        for (AudioDataPayload.Receiver r : payload.receivers()) {
            handlePlayData(payload.packetId(), payload.sourceId(),
                    r.x(), r.y(), r.z(), r.distance(), r.volume(), r.deviceId());
        }
    }
}