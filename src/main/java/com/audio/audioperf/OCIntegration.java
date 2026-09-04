package com.audio.audioperf;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.IMC;
import li.cil.oc.api.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.Callable;

public class OCIntegration {
    public static void registerTapeFloppy() {
        if (!ModList.get().isLoaded("opencomputers")) return;
        try {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("audio_perf", "loot/tape");
            // Test resource existence
            URL resourceUrl = AudioPerf.class.getResource("/assets/audio_perf/loot/tape/usr/bin/tape.lua");
            if (resourceUrl != null) {
                AudioPerf.LOGGER.info("tape.lua found at: {}", resourceUrl);
            } else {
                AudioPerf.LOGGER.error("tape.lua NOT found in classpath");
            }

            Callable<li.cil.oc.api.fs.FileSystem> factory = () -> {
                // Always create a memory filesystem with tape.lua to ensure it works
                AudioPerf.LOGGER.info("Creating memory filesystem with tape.lua");
                try (java.io.InputStream in = AudioPerf.class.getResourceAsStream("/assets/audio_perf/loot/tape/usr/bin/tape.lua")) {
                    if (in == null) {
                        AudioPerf.LOGGER.error("tape.lua not found in classpath");
                        return null;
                    }
                    byte[] content = in.readAllBytes();
                    long capacity = content.length + 1024;
                    li.cil.oc.api.fs.FileSystem memFs = FileSystem.fromMemory(capacity);
                    memFs.makeDirectory("/usr");
                    memFs.makeDirectory("/usr/bin");
                    try (java.io.OutputStream out = memFs.open("/usr/bin/tape.lua", li.cil.oc.api.fs.FileSystem.WriteMode.WRITE)) {
                        out.write(content);
                    }
                    // Verify the file was written
                    String[] rootFiles = memFs.list("/");
                    AudioPerf.LOGGER.info("Memory FS root contents: {}", String.join(", ", rootFiles));
                    try (java.io.InputStream verifyIn = memFs.open("/usr/bin/tape.lua", li.cil.oc.api.fs.FileSystem.ReadMode.READ)) {
                        if (verifyIn.available() == content.length) {
                            AudioPerf.LOGGER.info("Memory FS verification successful");
                        } else {
                            AudioPerf.LOGGER.warn("Memory FS verification failed: file size mismatch");
                        }
                    }
                    AudioPerf.LOGGER.info("Created memory filesystem with tape.lua");
                    return FileSystem.asReadOnly(memFs);
                } catch (Exception e) {
                    AudioPerf.LOGGER.error("Failed to create memory filesystem", e);
                    return null;
                }
            };
            Items.registerFloppy("tape", loc, DyeColor.WHITE, factory, true);
            IMC.registerProgramDiskLabel("tape", "tape", "Lua 5.2", "Lua 5.3", "LuaJ");
            AudioPerf.LOGGER.info("Registered tape program diskette");
        } catch (Exception e) {
            AudioPerf.LOGGER.warn("Failed to register tape floppy", e);
        }
    }
}