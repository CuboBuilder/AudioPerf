package com.audio.audioperf.audio;

public class DFPWMPlaybackManager extends StreamingPlaybackManager {
    public DFPWMPlaybackManager(boolean isClient) {
        super(isClient);
    }

    @Override
    public StreamingAudioPlayer create() {
        // DFPWM is mono, 8-bit, with 4 buffers
        return new StreamingAudioPlayer(true, false, 4);
    }
}
