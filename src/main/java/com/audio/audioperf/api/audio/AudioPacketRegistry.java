package com.audio.audioperf.api.audio;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class AudioPacketRegistry {
    public static final AudioPacketRegistry INSTANCE = new AudioPacketRegistry();
    
    private final Map<Class<? extends AudioPacket>, Integer> audioPacketIdMap = new HashMap<>();
    private final Map<Integer, Function<io.netty.buffer.ByteBuf, AudioPacket>> audioPacketDecoderMap = new HashMap<>();
    private int nextTypeId;
    
    private AudioPacketRegistry() {}
    
    public void registerType(Class<? extends AudioPacket> type, Function<io.netty.buffer.ByteBuf, AudioPacket> decoder) {
        int id = nextTypeId++;
        audioPacketIdMap.put(type, id);
        audioPacketDecoderMap.put(id, decoder);
    }
    
    public int getId(Class<? extends AudioPacket> packetClass) {
        return audioPacketIdMap.getOrDefault(packetClass, -1);
    }
    
    public AudioPacket decode(int id, io.netty.buffer.ByteBuf buffer) {
        Function<io.netty.buffer.ByteBuf, AudioPacket> decoder = audioPacketDecoderMap.get(id);
        return decoder != null ? decoder.apply(buffer) : null;
    }
}