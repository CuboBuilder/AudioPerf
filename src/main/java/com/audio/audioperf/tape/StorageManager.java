package com.audio.audioperf.tape;

import com.audio.audioperf.AudioPerf;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.File;
import java.security.SecureRandom;
import java.util.Random;

public class StorageManager {
    private static final Random RAND = new Random();

    private File saveDir() {
        File currentSaveRootDirectory = ServerLifecycleHooks.getCurrentServer().getServerDirectory().toFile();
        File saveDir = new File(currentSaveRootDirectory, AudioPerf.MODID);
        if (!saveDir.exists() && !saveDir.mkdir()) {
            AudioPerf.LOGGER.error("COULD NOT CREATE SAVE DIRECTORY: " + saveDir.getAbsolutePath());
        }
        return saveDir;
    }

    private String filename(String storageName) {
        return storageName + ".dsk";
    }

    public TapeStorage newStorage(int size) {
        String storageName;
        byte[] nameHex = new byte[16];
        do {
            RAND.nextBytes(nameHex);
            storageName = asHexString(nameHex);
        } while (exists(storageName));
        return get(storageName, size, 0);
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String asHexString(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    public boolean exists(String name) {
        return new File(saveDir(), filename(name)).exists();
    }

    public TapeStorage get(String name, int size, int position) {
        return new TapeStorage(name, new File(saveDir(), filename(name)), size, position);
    }
}