package com.audio.audioperf.tile;

import com.audio.audioperf.AudioPerf;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AudioPerfBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AudioPerf.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAudioCable>> AUDIO_CABLE =
            BLOCK_ENTITIES.register("audio_cable", () -> BlockEntityType.Builder.of(TileAudioCable::new, AudioPerf.AUDIO_CABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTapeDrive>> TAPE_DRIVE =
            BLOCK_ENTITIES.register("tape_drive", () -> BlockEntityType.Builder.of(TileTapeDrive::new, AudioPerf.TAPE_DRIVE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSpeaker>> SPEAKER =
            BLOCK_ENTITIES.register("speaker", () -> BlockEntityType.Builder.of(TileSpeaker::new, AudioPerf.SPEAKER.get()).build(null));
}