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
                // Try to load from OC resource system first
                li.cil.oc.api.fs.FileSystem fs = FileSystem.fromResource(loc);
                if (fs != null) {
                    AudioPerf.LOGGER.info("Loaded tape filesystem via fromResource");
                    return FileSystem.asReadOnly(fs);
                }
                // Try via fromClass reflection (OC's internal method)
                try {
                    java.lang.reflect.Method fromClass = li.cil.oc.api.FileSystem.class.getMethod("fromClass", Class.class, String.class, String.class);
                    fs = (li.cil.oc.api.fs.FileSystem) fromClass.invoke(null, AudioPerf.class, "audio_perf", "loot/tape");
                    if (fs != null) {
                        AudioPerf.LOGGER.info("Loaded tape filesystem via fromClass fallback");
                        return FileSystem.asReadOnly(fs);
                    }
                } catch (Exception e) {
                    AudioPerf.LOGGER.warn("fromClass fallback failed", e);
                }
                // Ultimate fallback: memory filesystem, read from classpath
                AudioPerf.LOGGER.info("Falling back to memory filesystem for tape.lua");
                java.io.InputStream in = null;
                String[] paths = {
                    "/assets/audio_perf/loot/tape/usr/bin/tape.lua",
                    "assets/audio_perf/loot/tape/usr/bin/tape.lua",
                    "/audio_perf/loot/tape/usr/bin/tape.lua",
                    "audio_perf/loot/tape/usr/bin/tape.lua"
                };
                for (String path : paths) {
                    AudioPerf.LOGGER.info("Trying resource path: {}", path);
                    in = AudioPerf.class.getResourceAsStream(path);
                    if (in != null) {
                        AudioPerf.LOGGER.info("Found tape.lua at: {}", path);
                        break;
                    }
                }
                if (in == null) {
                    AudioPerf.LOGGER.error("tape.lua not found in classpath with any variant");
                    return null;
                }
                try (in) {
                    byte[] content = in.readAllBytes();
                    long capacity = content.length + 1024;
                    li.cil.oc.api.fs.FileSystem memFs = FileSystem.fromMemory(capacity);
                    memFs.makeDirectory("/usr");
                    memFs.makeDirectory("/usr/bin");
                    int handle = memFs.open("/usr/bin/tape.lua", li.cil.oc.api.fs.Mode.Write);
                    li.cil.oc.api.fs.Handle h = memFs.getHandle(handle);
                    h.write(content);
                    h.close();
                    // Verify
                    String[] rootFiles = memFs.list("/");
                    AudioPerf.LOGGER.info("Memory FS root contents: {}", String.join(", ", rootFiles));
                    int readHandle = memFs.open("/usr/bin/tape.lua", li.cil.oc.api.fs.Mode.Read);
                    li.cil.oc.api.fs.Handle rh = memFs.getHandle(readHandle);
                    byte[] readContent = new byte[content.length];
                    int bytesRead = rh.read(readContent);
                    rh.close();
                    if (bytesRead == content.length) {
                        AudioPerf.LOGGER.info("Memory FS verification successful");
                    } else {
                        AudioPerf.LOGGER.warn("Memory FS verification failed: read {} bytes, expected {}", bytesRead, content.length);
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