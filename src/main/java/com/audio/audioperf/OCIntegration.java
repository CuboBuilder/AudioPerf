package com.audio.audioperf;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.IMC;
import li.cil.oc.api.Items;
import net.neoforged.fml.ModList;

public class OCIntegration {
    public static void registerTapeFloppy() {
        if (!ModList.get().isLoaded("opencomputers")) return;
        try {
            FileSystem fs = FileSystem.asReadOnly(
                FileSystem.fromClass(AudioPerf.class, "audio_perf", "loot/tape")
            );
            Items.registerFloppy("tape", 0, fs, true);
            IMC.registerProgramDiskLabel("tape", "tape", "Lua 5.2", "Lua 5.3", "LuaJ");
            AudioPerf.LOGGER.info("Registered tape program diskette");
        } catch (Exception e) {
            AudioPerf.LOGGER.warn("Failed to register tape floppy", e);
        }
    }
}