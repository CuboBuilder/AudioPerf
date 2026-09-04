package com.audio.audioperf;

import com.audio.audioperf.audio.ClientAudioHandler;
import com.audio.audioperf.tile.AudioPerfMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientLevelEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = AudioPerf.MODID, dist = Dist.CLIENT)
public class AudioPerfClient {

    public AudioPerfClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterScreens);
        NeoForge.EVENT_BUS.addListener(this::onClientDisconnect);
        NeoForge.EVENT_BUS.addListener(this::onClientLevelUnload);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientAudioHandler.create();
        });
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(AudioPerfMenus.TAPE_DRIVE.get(), TapeDriveScreen::new);
    }

    private void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAudioHandler handler = ClientAudioHandler.get();
        if (handler != null) {
            handler.getPlaybackManager().removeAll();
        }
    }

    private void onClientLevelUnload(ClientLevelEvent.Unload event) {
        ClientAudioHandler handler = ClientAudioHandler.get();
        if (handler != null) {
            handler.getPlaybackManager().removeAll();
        }
    }
}