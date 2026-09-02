package com.audio.audioperf.api.audio;

import net.minecraft.core.Direction;

public interface IAudioConnection {
    boolean connectsAudio(Direction side);
}