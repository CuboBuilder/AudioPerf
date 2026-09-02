package com.audio.audioperf.api.audio;

import io.netty.buffer.ByteBuf;

import java.util.function.Function;

public class AudioPacketDFPWM extends AudioPacket {
    public final int frequency;
    public final byte[] data;

    public AudioPacketDFPWM(IAudioSource source, byte volume, int frequency, byte[] data) {
        super(source, volume);
        this.frequency = frequency;
        this.data = data;
    }

    @Override
    protected void writeData(ByteBuf buffer) {
        buffer.writeInt(frequency);
        buffer.writeShort(data.length);
        buffer.writeBytes(data);
    }

    /**
     * Reconstructs the DFPWM packet payload data (frequency + data) from a
     * buffer, for client-side decoding. The source and volume are not part of
     * the serialized payload.
     */
    public static class Decoder implements Function<ByteBuf, AudioPacket> {
        @Override
        public AudioPacket apply(ByteBuf buffer) {
            int frequency = buffer.readInt();
            short length = buffer.readShort();
            byte[] data = new byte[length];
            buffer.readBytes(data);
            return new AudioPacketDFPWM(null, (byte) 0, frequency, data);
        }
    }
}