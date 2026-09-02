package com.audio.audioperf.audio;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.api.audio.AudioPacketRegistry;
import com.audio.audioperf.network.AudioStopPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AudioUtils {
    private AudioUtils() {}

    public static synchronized void removePlayer(int managerId, int codecId) {
        StreamingPlaybackManager manager = AudioPerf.instance().getAudioManager();
        if (manager != null) {
            manager.removePlayer(codecId);
        }
        PacketDistributor.sendToAllPlayers(new AudioStopPayload(managerId, codecId));
    }

    public static String positionId(int x, int y, int z) {
        return String.format("(%d, %d, %d)", x, y, z);
    }
}
