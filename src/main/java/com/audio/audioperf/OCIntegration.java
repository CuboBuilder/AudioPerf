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
                li.cil.oc.api.fs.FileSystem fs = FileSystem.fromResource(loc);
                if (fs == null) {
                    AudioPerf.LOGGER.error("fromResource returned null for {}", loc);
                    // Try with a different path: maybe it needs "loot/tape/"?
                    ResourceLocation loc2 = ResourceLocation.fromNamespaceAndPath("audio_perf", "loot/tape/");
                    fs = FileSystem.fromResource(loc2);
                    if (fs != null) {
                        AudioPerf.LOGGER.info("fromResource succeeded with trailing slash");
                    }
                }
                if (fs == null) {
                    // Fallback: try using reflection to call FileSystem.fromClass if available
                    try {
                        Method fromClass = li.cil.oc.api.FileSystem.class.getMethod("fromClass", Class.class, String.class, String.class);
                        fs = (li.cil.oc.api.fs.FileSystem) fromClass.invoke(null, AudioPerf.class, "audio_perf", "loot/tape");
                        if (fs != null) {
                            AudioPerf.LOGGER.info("Loaded tape filesystem via fromClass fallback");
                        }
                    } catch (Exception e) {
                        AudioPerf.LOGGER.warn("Fallback via fromClass failed", e);
                    }
                }
                if (fs == null) {
                    // Ultimate fallback: create a memory filesystem and copy tape.lua into it
                    AudioPerf.LOGGER.warn("All filesystem loading attempts failed; creating memory filesystem with tape.lua");
                    // Read tape.lua from classpath
                    try (java.io.InputStream in = AudioPerf.class.getResourceAsStream("/assets/audio_perf/loot/tape/usr/bin/tape.lua")) {
                        if (in == null) {
                            AudioPerf.LOGGER.error("tape.lua not found in classpath");
                            return null;
                        }
                        // Read the file into a byte array
                        byte[] content = in.readAllBytes();
                        // Create a memory filesystem with capacity (size of content + some overhead)
                        long capacity = content.length + 1024;
                        li.cil.oc.api.fs.FileSystem memFs = FileSystem.fromMemory(capacity);
                        // Create the directory structure
                        memFs.makeDirectory("/usr");
                        memFs.makeDirectory("/usr/bin");
                        // Write the file
                        try (java.io.OutputStream out = memFs.open("/usr/bin/tape.lua", li.cil.oc.api.fs.FileSystem.WriteMode.WRITE)) {
                            out.write(content);
                        }
                        AudioPerf.LOGGER.info("Created memory filesystem with tape.lua");
                        return FileSystem.asReadOnly(memFs);
                    } catch (Exception e) {
                        AudioPerf.LOGGER.error("Failed to create memory filesystem fallback", e);
                        return null;
                    }
                }
                // Log contents to verify
                try {
                    String[] files = fs.list("/");
                    AudioPerf.LOGGER.info("Tape filesystem root contents: {}", String.join(", ", files));
                } catch (Exception e) {
                    AudioPerf.LOGGER.warn("Could not list filesystem root", e);
                }
                return FileSystem.asReadOnly(fs);
            };
            Items.registerFloppy("tape", loc, DyeColor.WHITE, factory, true);
            IMC.registerProgramDiskLabel("tape", "tape", "Lua 5.2", "Lua 5.3", "LuaJ");
            AudioPerf.LOGGER.info("Registered tape program diskette");
        } catch (Exception e) {
            AudioPerf.LOGGER.warn("Failed to register tape floppy", e);
        }
    }
}