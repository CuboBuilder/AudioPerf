package com.audio.audioperf.api.audio;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IAudioReceiver extends IAudioConnection {
    Level getSoundWorld();
    Vec3 getSoundPos();
    int getSoundDistance();
    void receivePacket(AudioPacket packet, Direction side);
    String getID();
}