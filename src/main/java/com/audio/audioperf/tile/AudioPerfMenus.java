package com.audio.audioperf.tile;

import com.audio.audioperf.AudioPerf;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AudioPerfMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AudioPerf.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<TapeDriveMenu>> TAPE_DRIVE =
            MENUS.register("tape_drive", () -> IMenuTypeExtension.create((id, inv, buf) ->
                    new TapeDriveMenu(id, inv, (TileTapeDrive) inv.player.level().getBlockEntity(buf.readBlockPos()))));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}