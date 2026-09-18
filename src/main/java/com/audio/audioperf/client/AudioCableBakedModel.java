package com.audio.audioperf.client;

import com.audio.audioperf.tile.TileAudioCable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Renders a cased cable as a full cube of the casing block.
 * Applied to every audio cable variant model at bake time.
 */
public class AudioCableBakedModel extends BakedModelWrapper<BakedModel> {
    public AudioCableBakedModel(BakedModel original) {
        super(original);
    }

    private static @Nullable BlockState casingOf(ModelData data) {
        BlockState casing = data.get(TileAudioCable.CASING_PROPERTY);
        return (casing != null && !casing.isAir()) ? casing : null;
    }

    private static BakedModel casingModel(BlockState casing) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        return dispatcher.getBlockModel(casing);
    }

    @Override
    public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(@Nullable BlockState state,
            @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        BlockState casing = casingOf(data);
        if (casing != null) {
            return casingModel(casing).getQuads(casing, side, rand, ModelData.EMPTY, renderType);
        }
        return super.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState casing = casingOf(data);
        if (casing != null) {
            return casingModel(casing).getParticleIcon(ModelData.EMPTY);
        }
        return super.getParticleIcon(data);
    }

    @Override
    public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState casing = casingOf(data);
        if (casing != null) {
            return casingModel(casing).getRenderTypes(casing, rand, ModelData.EMPTY);
        }
        return super.getRenderTypes(state, rand, data);
    }
}
