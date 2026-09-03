package com.audio.audioperf;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.IMC;
import li.cil.oc.api.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.fml.ModList;

import java.util.concurrent.Callable;

public class OCIntegration {
    public static void registerTapeFloppy() {
        if (!ModList.get().isLoaded("opencomputers")) return;
        try {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("audio_perf", "loot/tape");
            Callable<li.cil.oc.api.fs.FileSystem> factory = () -> {
                li.cil.oc.api.fs.FileSystem fs = FileSystem.fromResource(loc);
                return fs != null ? FileSystem.asReadOnly(fs) : null;
            };
            Items.registerFloppy("tape", loc, DyeColor.WHITE, factory, true);
            IMC.registerProgramDiskLabel("tape", "tape", "Lua 5.2", "Lua 5.3", "LuaJ");
            AudioPerf.LOGGER.info("Registered tape program diskette");
        } catch (Exception e) {
            AudioPerf.LOGGER.warn("Failed to register tape floppy", e);
        }
    }
}