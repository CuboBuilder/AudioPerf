package com.audio.audioperf.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StreamingAudioPlayer extends DFPWM {
    private static final int AL_SOURCE_RELATIVE = 0x202;
    private static final int AL_LOOPING = 0x1007;
    private static final int AL_SOURCE_STATE = 0x1010;
    private static final int AL_PLAYING = 0x1012;
    private static final int AL_BUFFERS_PROCESSED = 0x1016;
    private static final int AL_ROLLOFF_FACTOR = 0x1021;

    private final Set<SourceEntry> sources = new HashSet<>();
    private final ArrayList<IntBuffer> buffersPlayed = new ArrayList<>();
    private final int bufferPackets;
    private final int audioFormat;

    private IntBuffer currentBuffer;
    private int sampleRate = 48000;
    private float volume = 1.0f;
    private float distance = 24.0f;

    public StreamingAudioPlayer(boolean mono, boolean sixteenBit, int bufferPackets) {
        this.bufferPackets = bufferPackets;
        if (mono) {
            this.audioFormat = sixteenBit ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_MONO8;
        } else {
            this.audioFormat = sixteenBit ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_STEREO8;
        }
        this.reset();
    }

    public void setHearing(float distance, float volume) {
        this.distance = distance;
        this.volume = volume;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void reset() {
        buffersPlayed.clear();
        stop();
    }

    public void updatePosition(String id, float x, float y, float z) {
        for (SourceEntry source : sources) {
            if (id != null && id.equals(source.id)) {
                AL10.alSource3f(source.src.get(0), AL10.AL_POSITION, x, y, z);
                return;
            }
        }
    }

    public void push(byte[] audio) {
        if (currentBuffer == null) {
            currentBuffer = BufferUtils.createIntBuffer(1);
        }
        for (SourceEntry source : sources) {
            int processed;
            while ((processed = AL10.alGetSourcei(source.src.get(0), AL_BUFFERS_PROCESSED)) > 0) {
                AL10.alSourceUnqueueBuffers(source.src.get(0), currentBuffer);
            }
        }

        AL10.alGenBuffers(currentBuffer);
        ByteBuffer data = BufferUtils.createByteBuffer(audio.length);
        data.put(audio);
        data.flip();
        AL10.alBufferData(currentBuffer.get(0), audioFormat, data, sampleRate);

        synchronized (buffersPlayed) {
            buffersPlayed.add(currentBuffer);
        }
    }

    public void play(int x, int y, int z) {
        play(null, x + 0.5f, y + 0.5f, z + 0.5f, 0);
    }

    public void play(int x, int y, int z, float pitch) {
        play(null, x + 0.5f, y + 0.5f, z + 0.5f, pitch);
    }

    public void play(String id, float x, float y, float z) {
        play(id, x, y, z, 0);
    }

    public void play(String id, float x, float y, float z, float pitch) {
        FloatBuffer position = BufferUtils.createFloatBuffer(3);
        position.put(new float[] {x, y, z});
        position.rewind();

        FloatBuffer velocity = BufferUtils.createFloatBuffer(3);
        velocity.put(new float[] {0, 0, 0});
        velocity.rewind();

        SourceEntry sourceEntry = null;
        for (SourceEntry entry : sources) {
            if (id != null && id.equals(entry.id)) {
                sourceEntry = entry;
                break;
            }
        }
        if (sourceEntry == null) {
            sourceEntry = new SourceEntry(id);
            sources.add(sourceEntry);
        }

        float dist = (float) getDistance(x, y, z);
        float hearing = distance * (0.2f + volume * 0.8f);
        // Volume is a continuous falloff by default; when a pitch is given it
        // acts as a simple in-range gate instead (matches original asielib).
        float vol;
        if (pitch > 0) {
            vol = (dist / hearing < 1f) ? 1f : 0f;
        } else {
            vol = 1f - dist / hearing;
        }
        float vol2 = vol * volume * Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
        if (vol2 < 0) vol2 = 0;
        if (vol2 > 1) vol2 = 1;

        int source = sourceEntry.src.get(0);
        AL10.alSourcei(source, AL_LOOPING, 0);
        AL10.alSourcef(source, AL10.AL_PITCH, 1.0f);
        AL10.alSourcef(source, AL10.AL_GAIN, vol2);
        AL10.alSourcefv(source, AL10.AL_POSITION, position);
        AL10.alSourcefv(source, AL10.AL_VELOCITY, velocity);
        AL10.alSourcef(source, AL_ROLLOFF_FACTOR, pitch * 6.0f / hearing);

        AL10.alSourceQueueBuffers(source, currentBuffer);

        int state = AL10.alGetSourcei(source, AL_SOURCE_STATE);
        if (sourceEntry.receivedPackets > bufferPackets && state != AL_PLAYING) {
            AL10.alSourcePlay(source);
        } else if (sourceEntry.receivedPackets <= bufferPackets) {
            AL10.alSourcePause(source);
        }
        sourceEntry.receivedPackets++;
    }

    public void stop() {
        int sourceCount = sources.size();
        Iterator<SourceEntry> it = sources.iterator();
        while (it.hasNext()) {
            SourceEntry entry = it.next();
            AL10.alSourceStop(entry.src.get(0));
            AL10.alDeleteSources(entry.src.get(0));
            it.remove();
        }
        synchronized (buffersPlayed) {
            if (currentBuffer != null) {
                buffersPlayed.add(currentBuffer);
            }
            Iterator<IntBuffer> itb = buffersPlayed.iterator();
            while (itb.hasNext()) {
                IntBuffer buffer = itb.next();
                buffer.rewind();
                for (int i = 0; i < buffer.limit(); i++) {
                    AL10.alDeleteBuffers(buffer.get(i));
                }
                itb.remove();
            }
        }
    }

    private double getDistance(double x, double y, double z) {
        net.minecraft.world.phys.Vec3 playerPos = Minecraft.getInstance().player.position();
        return Math.sqrt((playerPos.x - x) * (playerPos.x - x) + (playerPos.y - y) * (playerPos.y - y) + (playerPos.z - z) * (playerPos.z - z));
    }

    private class SourceEntry {
        public final String id;
        public final IntBuffer src;
        public int receivedPackets;

        public SourceEntry(String id) {
            this.id = id;
            this.src = BufferUtils.createIntBuffer(1);
            AL10.alGenSources(this.src);
        }
    }
}
