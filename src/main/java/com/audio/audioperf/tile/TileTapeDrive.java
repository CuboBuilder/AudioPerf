package com.audio.audioperf.tile;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.api.audio.AudioPacket;
import com.audio.audioperf.api.audio.AudioPacketDFPWM;
import com.audio.audioperf.api.audio.IAudioReceiver;
import com.audio.audioperf.api.audio.IAudioSource;
import com.audio.audioperf.api.tape.IItemTapeStorage;
import com.audio.audioperf.api.tape.ITapeStorage;
import com.audio.audioperf.audio.AudioUtils;
import com.audio.audioperf.tile.TapeDriveState.State;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.BlockEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class TileTapeDrive extends BlockEntityEnvironment implements IAudioSource, MenuProvider {

    private final IAudioReceiver internalSpeaker = new IAudioReceiver() {
        @Override
        public boolean connectsAudio(Direction side) { return true; }
        @Override
        public Level getSoundWorld() { return level; }
        @Override
        public Vec3 getSoundPos() {
            return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        }
        @Override
        public int getSoundDistance() { return 16; }
        @Override
        public void receivePacket(AudioPacket packet, Direction direction) {
            packet.addReceiver(this);
        }
        @Override
        public String getID() {
            return AudioUtils.positionId(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            onSlotUpdate();
        }
    };

    private TapeDriveState state;
    private String storageName = "";

    public TileTapeDrive(BlockPos pos, BlockState state) {
        super(AudioPerfBlockEntities.TAPE_DRIVE.get(), pos, state);
        this.state = new TapeDriveState();
        this.node = Network.newNode(this, Visibility.Network)
                .withComponent("tape_drive", Visibility.Neighbors)
                .create();
    }

    // ========== Inventory Access ==========

    public IItemHandler getInventory() {
        return inventory;
    }

    public ItemStack getTapeStack() {
        return inventory.getStackInSlot(0);
    }

    // ========== Slot update handling ==========

    private void onSlotUpdate() {
        if (level == null || level.isClientSide) return;
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            if (state.getStorage() != null) {
                // Tape removed
                if (level != null) {
                    level.playSound(null, worldPosition, AudioPerf.TAPE_EJECT_SOUND.get(),
                            net.minecraft.sounds.SoundSource.BLOCKS, 1, 0);
                }
            }
            unloadStorage();
        } else {
            loadStorage();
            if (stack.getItem() instanceof IItemTapeStorage) {
                if (level != null) {
                    level.playSound(null, worldPosition, AudioPerf.TAPE_INSERT_SOUND.get(),
                            net.minecraft.sounds.SoundSource.BLOCKS, 1, 0);
                }
            }
        }
    }

    // ========== State Access ==========

    public State getEnumState() { return state.getState(); }

    public void switchState(State s) {
        if (getEnumState() != s) {
            state.switchState(level, s);
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                setChanged();
                // Immediately sync state to clients via custom packet
                if (level instanceof ServerLevel serverLevel) {
                    PacketDistributor.sendToPlayersTrackingChunk(serverLevel, level.getChunkAt(worldPosition),
                        new com.audio.audioperf.network.TapeDriveStateSyncPayload(worldPosition, (byte) s.ordinal()));
                }
            }
        }
    }

    public void onClientStateSync(State s) {
        if (level != null && level.isClientSide) {
            state.setState(s);
        }
    }

    public void setSpeed(float speed) { state.setSpeed(speed); }
    public void setVolume(float vol) { state.setVolume(vol); }

    public boolean isEnd() {
        return state.getStorage() == null || state.getStorage().getPosition() + state.packetSize > state.getStorage().getSize();
    }

    public boolean isReady() { return state.getStorage() != null; }
    public int getSize() { return state.getStorage() != null ? state.getStorage().getSize() : 0; }
    public int getPosition() { return state.getStorage() != null ? state.getStorage().getPosition() : 0; }
    public int seek(int bytes) { return state.getStorage() != null ? state.getStorage().seek(bytes) : 0; }
    public int read() { return read(false); }
    public int read(boolean simulate) {
        return state.getStorage() != null ? state.getStorage().read(simulate) & 0xFF : 0;
    }
    public byte[] read(int amount) {
        if (state.getStorage() != null) {
            byte[] data = new byte[amount];
            state.getStorage().read(data, false);
            return data;
        }
        return null;
    }
    public void write(byte b) { if (state.getStorage() != null) state.getStorage().write(b); }
    public int write(byte[] bytes) { return state.getStorage() != null ? state.getStorage().write(bytes) : 0; }

    // ========== Tile Entity Tick ==========

    public void tick() {
        if (level == null) return;
        State st = getEnumState();
        AudioPacket pkt = state.update(this, level);
        if (pkt != null) {
            for (Direction dir : Direction.values()) {
                BlockEntity tile = level.getBlockEntity(worldPosition.relative(dir));
                if (tile instanceof IAudioReceiver receiver) {
                    receiver.receivePacket(pkt, dir.getOpposite());
                }
            }
            // Only use the internal speaker when no external speaker received the
            // packet (e.g. when connected to a cable with no speaker at the end).
            if (pkt.getReceivers().isEmpty()) {
                internalSpeaker.receivePacket(pkt, Direction.UP);
            }
            pkt.sendPacket();
        }
        // Play the rewind sound once when entering rewind/forward, and stop it
        // when leaving (prevents overlapping loops).
        if (!level.isClientSide) {
            boolean seeking = getEnumState() == State.REWINDING || getEnumState() == State.FORWARDING;
            boolean wasSeeking = st == State.REWINDING || st == State.FORWARDING;
            if (seeking && !wasSeeking) {
                level.playSound(null, worldPosition, AudioPerf.TAPE_REWIND_SOUND.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        if (!level.isClientSide && st != getEnumState()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();
        }
    }

    // ========== Storage Management ==========

    private void loadStorage() {
        if (level != null && level.isClientSide) return;
        if (state.getStorage() != null) unloadStorage();
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            if (stack.getItem() instanceof IItemTapeStorage tapeItem) {
                state.setStorage(tapeItem.getStorage(stack));
            }
            CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            storageName = tag != null && tag.contains("label") ? tag.getString("label") : "";
        }
    }

    private void unloadStorage() {
        if (level != null && level.isClientSide) return;
        if (state.getStorage() == null) return;
        switchState(State.STOPPED);
        try {
            state.getStorage().onStorageUnload();
        } catch (Exception e) {
            e.printStackTrace();
        }
        state.setStorage(null);
    }

    // ========== Lifecycle ==========

    @Override
    public void setRemoved() {
        unloadStorage();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unloadStorage();
        super.onChunkUnloaded();
    }

    // ========== NBT ==========

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("state")) {
            state.setState(State.VALUES[tag.getByte("state")]);
        }
        if (tag.contains("sp")) {
            state.packetSize = tag.getShort("sp");
        }
        if (tag.contains("vo")) {
            state.soundVolume = tag.getByte("vo");
        } else {
            state.soundVolume = 127;
        }
        if (tag.contains("inv")) {
            inventory.deserializeNBT(registries, tag.getCompound("inv"));
        }
        loadStorage();
        // If there is no tape, the drive must not resume a playback/seek state.
        if (state.getStorage() == null) {
            state.setState(State.STOPPED);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("sp", (short) state.packetSize);
        tag.putByte("state", (byte) state.getState().ordinal());
        if (state.soundVolume != 127) {
            tag.putByte("vo", (byte) state.soundVolume);
        }
        tag.put("inv", inventory.serializeNBT(registries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putByte("state", (byte) state.getState().ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("state")) {
            state.setState(State.VALUES[tag.getByte("state")]);
        }
    }

    // ========== IAudioSource ==========

    @Override
    public int getSourceId() { return state.getId(); }

    @Override
    public boolean connectsAudio(Direction side) {
        return side != getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
    }

    // ========== Menu Provider ==========

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.audio_perf.tape_drive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new TapeDriveMenu(id, playerInv, this);
    }

    // ========== OpenComputers Callbacks ==========

    @Callback(doc = "function():boolean; Returns true if the tape drive is empty or the inserted tape has reached its end", direct = true)
    public Object[] isEnd(Context context, Arguments args) {
        return new Object[]{isEnd()};
    }

    @Callback(doc = "function():boolean; Returns true if there is a tape inserted", direct = true)
    public Object[] isReady(Context context, Arguments args) {
        return new Object[]{isReady()};
    }

    @Callback(doc = "function():number; Returns the size of the tape, in bytes", direct = true)
    public Object[] getSize(Context context, Arguments args) {
        return new Object[]{getSize()};
    }

    @Callback(doc = "function():number; Returns the position of the tape, in bytes", direct = true)
    public Object[] getPosition(Context context, Arguments args) {
        return new Object[]{getPosition()};
    }

    @Callback(doc = "function(label:string):string; Sets the label of the tape. Returns the new label, or nil if there is no tape inserted")
    public Object[] setLabel(Context context, Arguments args) {
        setLabel(args.checkString(0));
        return new Object[]{state.getStorage() != null ? storageName : null};
    }

    @Callback(doc = "function():string; Returns the current label of the tape, or nil if there is no tape inserted")
    public Object[] getLabel(Context context, Arguments args) {
        return new Object[]{state.getStorage() != null ? storageName : null};
    }

    @Callback(doc = "function(length:number):number; Seeks the specified amount of bytes on the tape. Negative values for rewinding. Returns the amount of bytes sought, or nil if there is no tape inserted")
    public Object[] seek(Context context, Arguments args) {
        if (state.getStorage() != null) {
            return new Object[]{seek(args.checkInteger(0))};
        }
        return null;
    }

    @Callback(doc = "function([length:number]):string; Reads and returns the specified amount of bytes or a single byte from the tape. Returns nil if there is no tape inserted")
    public Object[] read(Context context, Arguments args) {
        if (state.getStorage() != null) {
            if (args.count() >= 1 && args.isInteger(0) && args.checkInteger(0) >= 0) {
                return new Object[]{read(args.checkInteger(0))};
            } else {
                return new Object[]{read()};
            }
        }
        return null;
    }

    @Callback(doc = "function(data:number or string); Writes the specified data to the tape if there is one inserted")
    public Object[] write(Context context, Arguments args) {
        if (state.getStorage() != null && args.count() >= 1) {
            if (args.isInteger(0)) {
                write((byte) args.checkInteger(0));
            } else if (args.isByteArray(0)) {
                write(args.checkByteArray(0));
            } else {
                throw new IllegalArgumentException("bad argument #1 (number or string expected)");
            }
        }
        return null;
    }

    @Callback(doc = "function():boolean; Make the Tape Drive start playing the tape. Returns true on success")
    public Object[] play(Context context, Arguments args) {
        switchState(State.PLAYING);
        return new Object[]{state.getStorage() != null && getEnumState() == State.PLAYING};
    }

    @Callback(doc = "function():boolean; Make the Tape Drive stop playing the tape. Returns true on success")
    public Object[] stop(Context context, Arguments args) {
        switchState(State.STOPPED);
        return new Object[]{state.getStorage() != null && getEnumState() == State.STOPPED};
    }

    @Callback(doc = "function(speed:number):boolean; Sets the speed of the tape drive. Needs to be between 0.25 and 2. Returns true on success")
    public Object[] setSpeed(Context context, Arguments args) {
        return new Object[]{state.setSpeed((float) args.checkDouble(0))};
    }

    @Callback(doc = "function(speed:number); Sets the volume of the tape drive. Needs to be between 0 and 1")
    public Object[] setVolume(Context context, Arguments args) {
        state.setVolume((float) args.checkDouble(0));
        return null;
    }

    @Callback(doc = "function():string; Returns the current state of the tape drive", direct = true)
    public Object[] getState(Context context, Arguments args) {
        return new Object[]{state.getState().toString()};
    }

    private void setLabel(String label) {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (label.isEmpty()) {
                tag.remove("label");
            } else {
                tag.putString("label", label);
            }
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            storageName = label;
        }
    }
}