package com.audio.audioperf.tile;

import com.audio.audioperf.api.audio.AudioPacket;
import com.audio.audioperf.api.audio.IAudioSource;
import com.audio.audioperf.api.tape.ITapeStorage;
import com.audio.audioperf.audio.AudioUtils;
import com.audio.audioperf.api.audio.AudioPacketDFPWM;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.Arrays;

public class TapeDriveState {
    public enum State {
        STOPPED,
        PLAYING,
        REWINDING,
        FORWARDING;
        public static final State[] VALUES = values();
    }

    private State state = State.STOPPED;
    private int codecId;
    private long lastCodecTime;
    public int packetSize = 1500;
    public int soundVolume = 127;
    private ITapeStorage storage;

    public ITapeStorage getStorage() { return storage; }
    public void setStorage(ITapeStorage storage) { this.storage = storage; }
    public void setState(State state) { this.state = state; }
    public State getState() { return state; }

    public boolean setSpeed(float speed) {
        if (speed < 0.25F || speed > 2.0F) return false;
        this.packetSize = Math.round(1500 * speed);
        return true;
    }

    public int getId() { return codecId; }

    public byte getVolume() { return (byte) soundVolume; }

    public void setVolume(float volume) {
        if (volume < 0.0F) volume = 0.0F;
        if (volume > 1.0F) volume = 1.0F;
        this.soundVolume = (int) Math.floor(volume * 127.0F);
    }

    public void switchState(Level worldObj, State newState) {
        if (worldObj.isClientSide) {
            if (newState == state) return;
        }
        if (!worldObj.isClientSide) {
            if (this.storage == null) newState = State.STOPPED;
            if (state == State.PLAYING) {
                AudioUtils.removePlayer(0, codecId);
            }
            if (newState == State.PLAYING) {
                codecId = (int) System.nanoTime(); // unique id
                lastCodecTime = System.nanoTime();
            }
        }
        state = newState;
    }

    private AudioPacket createMusicPacket(IAudioSource source, Level worldObj) {
        byte[] pktData = new byte[packetSize];
        int amount = storage.read(pktData, false);
        if (amount < packetSize) switchState(worldObj, State.STOPPED);
        if (amount > 0) {
            return new AudioPacketDFPWM(source, getVolume(), 48000,
                    amount == packetSize ? pktData : Arrays.copyOf(pktData, amount));
        } else {
            return null;
        }
    }

    public AudioPacket update(IAudioSource source, Level worldObj) {
        if (!worldObj.isClientSide) {
            if (storage == null) {
                if (state != State.STOPPED) {
                    state = State.STOPPED;
                }
                return null;
            }
            switch (state) {
                case PLAYING: {
                    if (storage.getPosition() >= storage.getSize() || storage.getPosition() < 0) {
                        storage.setPosition(storage.getPosition());
                    }
                    long time = System.nanoTime();
                    if ((time - (250 * 1000000L)) > lastCodecTime) {
                        lastCodecTime += (250 * 1000000L);
                        return createMusicPacket(source, worldObj);
                    }
                } break;
                case REWINDING: {
                    int seeked = storage.seek(-2048);
                    if (seeked > -2048) {
                        if (source instanceof TileTapeDrive tile) {
                            tile.switchState(State.STOPPED);
                        } else {
                            switchState(worldObj, State.STOPPED);
                        }
                    }
                } break;
                case FORWARDING: {
                    int seeked = storage.seek(2048);
                    if (seeked < 2048) {
                        if (source instanceof TileTapeDrive tile) {
                            tile.switchState(State.STOPPED);
                        } else {
                            switchState(worldObj, State.STOPPED);
                        }
                    }
                } break;
                default: break;
            }
        }
        return null;
    }
}