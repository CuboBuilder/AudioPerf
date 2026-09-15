package com.audio.audioperf.block;

import com.audio.audioperf.api.audio.IAudioColored;
import com.audio.audioperf.network.AudioCableColorPayload;
import com.audio.audioperf.tile.TileAudioCable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shared dye-painting logic for colored audio blocks. White dye resets to the default color. */
public final class AudioPaintHelper {
    private AudioPaintHelper() {}

    public static ItemInteractionResult paint(Level level, BlockPos pos, Player player,
                                              ItemStack stack, DyeItem dye, IAudioColored target) {
        if (!level.isClientSide) {
            int color = IAudioColored.dyeColor(dye);
            if (target.getColor() != color) {
                target.setColor(color);
                if (target instanceof BlockEntity be) {
                    be.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                }
                // Neighboring cables may connect or disconnect.
                for (Direction dir : Direction.values()) {
                    if (level.getBlockEntity(pos.relative(dir)) instanceof TileAudioCable cable) {
                        cable.refreshConnections();
                    }
                }
                if (level instanceof ServerLevel serverLevel) {
                    PacketDistributor.sendToPlayersTrackingChunk(
                            serverLevel, new net.minecraft.world.level.ChunkPos(pos),
                            new AudioCableColorPayload(pos, color));
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @SuppressWarnings("unused")
    public static ItemInteractionResult paint(Level level, BlockPos pos, Player player,
                                              InteractionHand hand, IAudioColored target) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof DyeItem dye) {
            return paint(level, pos, player, stack, dye, target);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
