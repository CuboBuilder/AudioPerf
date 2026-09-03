package com.audio.audioperf;

import com.audio.audioperf.api.audio.AudioPacketDFPWM;
import com.audio.audioperf.api.audio.AudioPacketRegistry;
import com.audio.audioperf.audio.DFPWMPlaybackManager;
import com.audio.audioperf.block.AudioCableBlock;
import com.audio.audioperf.block.SpeakerBlock;
import com.audio.audioperf.block.TapeDriveBlock;
import com.audio.audioperf.item.ItemTape;
import com.audio.audioperf.network.AudioDataPayload;
import com.audio.audioperf.network.AudioStopPayload;
import com.audio.audioperf.network.TapeDriveStatePayload;
import com.audio.audioperf.network.TapeDriveStateSyncPayload;
import com.audio.audioperf.tape.StorageManager;
import com.audio.audioperf.tile.AudioPerfBlockEntities;
import com.audio.audioperf.tile.AudioPerfMenus;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(AudioPerf.MODID)
public class AudioPerf {
    public static final String MODID = "audio_perf";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static AudioPerf instance;
    private static net.minecraft.server.MinecraftServer serverInstance;

    public static AudioPerf instance() {
        return instance;
    }

    public static net.minecraft.server.MinecraftServer getServer() {
        return serverInstance;
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    // Blocks
    public static final DeferredBlock<Block> AUDIO_CABLE = BLOCKS.register("audio_cable", () -> new AudioCableBlock());
    public static final DeferredBlock<Block> TAPE_DRIVE = BLOCKS.register("tape_drive", () -> new TapeDriveBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0f)
            .sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final DeferredBlock<Block> SPEAKER = BLOCKS.register("speaker", () -> new SpeakerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f)
            .sound(net.minecraft.world.level.block.SoundType.WOOD)));

    // Block items
    public static final DeferredItem<BlockItem> AUDIO_CABLE_ITEM = ITEMS.registerSimpleBlockItem("audio_cable", AUDIO_CABLE);
    public static final DeferredItem<BlockItem> TAPE_DRIVE_ITEM = ITEMS.registerSimpleBlockItem("tape_drive", TAPE_DRIVE);
    public static final DeferredItem<BlockItem> SPEAKER_ITEM = ITEMS.registerSimpleBlockItem("speaker", SPEAKER);

    // Items
    public static final DeferredItem<ItemTape> TAPE = ITEMS.register("tape", () -> new ItemTape(
            new Item.Properties().stacksTo(1)));

    // Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_INSERT_SOUND =
            SOUNDS.register("tape_insert", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "tape_insert")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_EJECT_SOUND =
            SOUNDS.register("tape_eject", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "tape_eject")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_REWIND_SOUND =
            SOUNDS.register("tape_rewind", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "tape_rewind")));

    // Creative tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.audio_perf"))
                    .icon(() -> TAPE_DRIVE_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(AUDIO_CABLE_ITEM.get());
                        output.accept(TAPE_DRIVE_ITEM.get());
                        output.accept(SPEAKER_ITEM.get());
                        for (int i = 0; i < ItemTape.TAPE_COUNT; i++) {
                            output.accept(TAPE.get().withIndex(i));
                        }
                    })
                    .build());

    private final DFPWMPlaybackManager audioManager;
    private final StorageManager storage;

    public AudioPerf(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUNDS.register(modEventBus);
        AudioPerfBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        AudioPerfMenus.MENUS.register(modEventBus);

        this.audioManager = new DFPWMPlaybackManager(false);
        this.storage = new StorageManager();

        OCIntegration.registerTapeFloppy();

        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        AudioPacketRegistry.INSTANCE.registerType(AudioPacketDFPWM.class, new AudioPacketDFPWM.Decoder());
    }

    private void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                AudioPerfBlockEntities.TAPE_DRIVE.get(),
                (be, side) -> ((com.audio.audioperf.tile.TileTapeDrive) be).getInventory());

        // Register OC Environment capability for the tape drive so OC's adapter
        // blocks can discover it. Capabilities are cached by name in NeoForge,
        // so creating one with the same ID as OC's returns the same instance.
        net.neoforged.neoforge.capabilities.BlockCapability<li.cil.oc.api.network.Environment, net.minecraft.core.Direction> envCap =
                net.neoforged.neoforge.capabilities.BlockCapability.createSided(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("opencomputers", "environment"),
                        li.cil.oc.api.network.Environment.class);
        event.registerBlockEntity(
                envCap,
                AudioPerfBlockEntities.TAPE_DRIVE.get(),
                (be, side) -> (li.cil.oc.api.network.Environment) be);
    }

    private void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        serverInstance = event.getServer();
    }

    private void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        if (audioManager != null) {
            audioManager.removeAll();
        }
        serverInstance = null;
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(AudioDataPayload.TYPE, AudioDataPayload.STREAM_CODEC, AudioDataPayload::handle);
        registrar.playToClient(AudioStopPayload.TYPE, AudioStopPayload.STREAM_CODEC, AudioStopPayload::handle);
        registrar.playToClient(TapeDriveStateSyncPayload.TYPE, TapeDriveStateSyncPayload.STREAM_CODEC, TapeDriveStateSyncPayload::handle);
        registrar.playToServer(TapeDriveStatePayload.TYPE, TapeDriveStatePayload.STREAM_CODEC, TapeDriveStatePayload::handle);
    }

    public DFPWMPlaybackManager getAudioManager() {
        return audioManager;
    }

    public StorageManager getStorage() {
        return storage;
    }
}
