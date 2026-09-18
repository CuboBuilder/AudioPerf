package com.audio.audioperf;

import com.audio.audioperf.audio.ClientAudioHandler;
import com.audio.audioperf.tile.AudioPerfMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = AudioPerf.MODID, dist = Dist.CLIENT)
public class AudioPerfClient {

    public AudioPerfClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterScreens);
        modEventBus.addListener(this::onRegisterBlockColors);
        modEventBus.addListener(this::onModifyBakingResult);
        NeoForge.EVENT_BUS.addListener(this::onClientDisconnect);
        NeoForge.EVENT_BUS.addListener(this::registerLootDisks);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientAudioHandler.create();
        });
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(AudioPerfMenus.TAPE_DRIVE.get(), TapeDriveScreen::new);
    }

    private void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex == 0 && level != null && pos != null && level.getBlockEntity(pos) instanceof com.audio.audioperf.tile.TileAudioCable cable) {
                        return cable.getColor() | 0xFF000000;
                    }
                    return 0xFFCCCCCC;
                }, AudioPerf.AUDIO_CABLE.get());
        event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex == 0 && level != null && pos != null && level.getBlockEntity(pos) instanceof com.audio.audioperf.api.audio.IAudioColored colored
                            && colored.getColor() != com.audio.audioperf.api.audio.IAudioColored.DEFAULT_COLOR) {
                        return colored.getColor() | 0xFF000000;
                    }
                    // Unpainted machines render untinted so existing textures look unchanged.
                    return 0xFFFFFFFF;
                }, AudioPerf.SPEAKER.get(), AudioPerf.TAPE_DRIVE.get());
    }

    private void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // Wrap every cable variant so cased cables render as the casing block.
        for (var entry : event.getModels().entrySet()) {
            net.minecraft.client.resources.model.ModelResourceLocation key = entry.getKey();
            if (key.getNamespace().equals(AudioPerf.MODID) && key.getPath().startsWith("block/audio_cable/")) {
                entry.setValue(new com.audio.audioperf.client.AudioCableBakedModel(entry.getValue()));
            }
        }
    }

    private void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAudioHandler handler = ClientAudioHandler.get();
        if (handler != null) {
            handler.getPlaybackManager().removeAll();
        }
    }

    private void registerLootDisks(final ClientTickEvent.Pre event) {
        if (li.cil.oc.api.API.items != null) {
            NeoForge.EVENT_BUS.unregister(this);
            registerTapeLootDisk();
        }
    }

    private void registerTapeLootDisk() {
        net.minecraft.resources.ResourceLocation lootPath = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AudioPerf.MODID, "loot/tape");
        li.cil.oc.api.API.items.registerFloppy(
                "tape",
                "tape",
                lootPath,
                net.minecraft.world.item.DyeColor.WHITE,
                () -> li.cil.oc.api.FileSystem.fromResource(lootPath),
                false
        );
    }
}