package li.cil.oc.api;

import li.cil.oc.api.datacomponents.MutableNbtComponentHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.OverridingMethodsMustInvokeSuper;

/**
 * An object that can be persisted to an NBT tag and restored back from it.
 */
public interface Persistable {
    /**
     * Explicitly tells this instance to load its data from the provided
     * {@link DataComponentHolder}. This could be anything really,
     * {@linkplain #loadData(CompoundTag, HolderLookup.Provider) including
     * entities}.
     *
     * <p>If you're trying to load data from a {@link BlockEntity}, see
     * {@link #holder(BlockEntity)}</p>
     *
     * <p>You should almost <b>never</b> throw an exception from this method.
     * If something is wrong, correct it or pretend it doesn't exist and move on.
     * Only instances of {@link UnrecoverablePersistanceException} may be thrown
     * which indicate data is known to be present, but it is not in a format that
     * could be understood at all <b>and</b> loading a default state is not possible.</p>
     *
     * @param holder The holder to load from.
     * @see #loadData(CompoundTag, HolderLookup.Provider)
     */
    @ApiStatus.AvailableSince("1.9; NeoForge 1.21.1+")
    void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException;

    /**
     * Explicitly tells this instance to make sure the data stored in
     * <code>holder</code> reflects the current state. The provided holder
     * could be anything really,
     * {@linkplain #loadData(CompoundTag, HolderLookup.Provider) including entities}.
     *
     * <p>If you're trying to load data from a {@link BlockEntity}, see
     * {@link #holder(BlockEntity)}</p>
     *
     * <p>There is no good reason to throw an exception in this method. It will
     * probably cause data corruption and cause your players to pull their hair
     * out over losing stuff. 10/10 would not recommend.</p>
     *
     * @param holder The mutable holder to save to. In some cases, this may be
     *               completely empty at the time of the call. Others, it may
     *               be an {@link ItemStack} that contains Minecraft's
     *               {@linkplain DataComponents registered data components}.
     *
     *               <p>Make sure you don't overwrite things you shouldn't.
     *               Register your own components if you need to.</p>
     */
    @ApiStatus.AvailableSince("1.9; NeoForge 1.21.1+")
    void saveData(MutableDataComponentHolder holder);

    /**
     * This alternative to {@link #loadData(DataComponentHolder)}
     * is provided to help simplify {@link Entity} implementations, which
     * may not store data in a neat and tidy {@link DataComponentMap}.
     *
     * <p>Implementations of {@link BlockEntity} <b>should</b> instead use
     * {@link Persistable#holder(BlockEntity)}, as block
     * entities need just a bit of extra work to get to that
     * {@link DataComponentMap} goodness.</p>
     *
     * <p>You may wish to override this method. Don't. Only Entities might
     * see your overridden version and any relevant migration is better
     * suited to be placed in Entity#readAdditionalSaveData.
     * If you absolutely must, always call super.</p>
     *
     * <p>You may wish to call this method with a subtag to avoid potential
     * conflicts with other parts of the code. When loading, if the tag does
     * not already exist, <b>please still call this method with an empty tag!</b></p>
     *
     * @param tag The tag.
     * @param provider A {@link HolderLookup.Provider} provided by the game.
     *                 On {@link Entity} instances, this can be accessed via
     *                 <code>this.level().registryAccess()</code>.
     */
    @OverridingMethodsMustInvokeSuper
    @ApiStatus.NonExtendable
    @ApiStatus.Obsolete(since = "OpenComputers 1.9, NeoForge 1.21.1+")
    default void loadData(CompoundTag tag, HolderLookup.Provider provider) throws UnrecoverablePersistanceException {
        loadData(new MutableNbtComponentHolder(tag, provider));
    }

    /**
     * This alternative to {@link #saveData(MutableDataComponentHolder)}
     * is provided to help simplify {@link Entity} implementations, which
     * may not store data in a neat and tidy {@link DataComponentMap}.
     *
     * <p>Implementations of {@link BlockEntity} <b>should</b> instead use
     * {@link Persistable#holder(BlockEntity)}, as block
     * entities need just a bit of extra work to get to that
     * {@link DataComponentMap} goodness.</p>
     *
     * <p>You may wish to override this method. Don't. Only Entities might
     * see your overridden version and any relevant migration is better
     * suited to be placed in Entity#readAdditionalSaveData.
     * If you absolutely must, always call super.</p>
     *
     * <p>You may wish to call this method with a subtag to avoid potential
     * conflicts with other parts of the code.</p>
     *
     * @param tag The tag.
     * @param provider A {@link HolderLookup.Provider} provided by the game.
     *                 On {@link Entity} instances, this can be accessed via
     *                 <code>this.level().registryAccess()</code>.
     */
    @OverridingMethodsMustInvokeSuper
    @ApiStatus.NonExtendable
    @ApiStatus.Obsolete(since = "1.9; NeoForge 1.21.1+")
    default void saveData(CompoundTag tag, HolderLookup.Provider provider) {
        var holder = new MutableNbtComponentHolder();
        saveData(holder);
        holder.save(tag, provider);
    }

    @ApiStatus.Internal
    interface BlockEntityMutableComponentHolder extends MutableDataComponentHolder, AutoCloseable {}

    @Contract(value = "_ -> new", pure = true)
    @ApiStatus.AvailableSince("1.9; NeoForge 1.21.1+")
    static @NonNull BlockEntityMutableComponentHolder holder(@NonNull BlockEntity blockEntity) {
        return new BlockEntityMutableComponentHolder() {
            private final DataComponentMap original = blockEntity.collectComponents();
            private final PatchedDataComponentMap patched = new PatchedDataComponentMap(original);

            @Override
            public void close() {
                blockEntity.applyComponents(original, patched.asPatch());
            }

            @Override
            public @Nullable <T> T set(@NonNull DataComponentType<? super T> componentType, @Nullable T value) {
                return patched.set(componentType, value);
            }

            @Override
            public @Nullable <T> T remove(@NonNull DataComponentType<? extends T> componentType) {
                return patched.remove(componentType);
            }

            @Override
            public void applyComponents(@NonNull DataComponentPatch patch) {
                patched.applyPatch(patch);
            }

            @Override
            public void applyComponents(@NonNull DataComponentMap components) {
                patched.setAll(components);
            }

            @Override
            public @NonNull DataComponentMap getComponents() {
                return patched;
            }
        };
    }

    /**
     * Equivalent to <code>v.loadData(Persistable.holder(map))</code>
     * 
     * @see #loadData(DataComponentHolder) 
     */
    default void loadData(DataComponentMap map) throws UnrecoverablePersistanceException {
        loadData(holder(map));
    }

    @Contract(value = "_ -> new", pure = true)
    @ApiStatus.AvailableSince("1.9; NeoForge 1.21.1+")
    static @NonNull DataComponentHolder holder(@NonNull DataComponentMap map) {
        return new DataComponentHolder() {
            @Override
            public @NonNull DataComponentMap getComponents() {
                return map;
            }
        };
    }

    @ApiStatus.AvailableSince("1.9; NeoForge 1.21.1+")
    DataComponentHolder EMPTY_HOLDER = holder(DataComponentMap.EMPTY);
}
