package li.cil.oc.api;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;

/**
 * This is a wrapper over {@link ItemStack} which is suitable for use in
 * Data Components, which are required to be immutable.
 */
public final class ImmutableItemStack implements DataComponentHolder {
    private final @NonNull ItemStack stack;

    public static final Codec<ImmutableItemStack> CODEC =
            ItemStack.CODEC.xmap(ImmutableItemStack::new, ImmutableItemStack::getStackUnsafe);
    public static final Codec<ImmutableItemStack> OPTIONAL_CODEC =
            ItemStack.OPTIONAL_CODEC.xmap(ImmutableItemStack::new, ImmutableItemStack::getStackUnsafe);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImmutableItemStack> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(ImmutableItemStack::new, ImmutableItemStack::getStackUnsafe);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImmutableItemStack> OPTIONAL_STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.map(ImmutableItemStack::new, ImmutableItemStack::getStackUnsafe);

    public static final ImmutableItemStack EMPTY = new ImmutableItemStack(ItemStack.EMPTY);

    private ImmutableItemStack(@NonNull ItemStack stack) {
        // Data component values must be immutable and have stable equals/hashCode.
        // Keeping a live ItemStack reference here allowed robot component stacks
        // (including their persisted node address) to mutate underneath the
        // DataComponentMap without Minecraft seeing a component change.
        this.stack = stack.copy();
    }

    private ItemStack getStackUnsafe() {
        return stack;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NonNull ImmutableItemStack copyOf(@NonNull ItemStack stack) {
        return new ImmutableItemStack(stack);
    }

    @Contract(pure = true)
    public @NonNull Item getItem() {
        return stack.getItem();
    }

    @Contract(pure = true)
    public @NonNull Holder<Item> getItemHolder() {
        return stack.getItemHolder();
    }

    @Contract(pure = true)
    public int getCount() {
        return stack.getCount();
    }

    @Contract("_ -> new")
    public @NonNull ImmutableItemStack copyWithCount(int count) {
        return new ImmutableItemStack(stack.copyWithCount(count));
    }

    @Override
    public @NonNull DataComponentMap getComponents() {
        return stack.getComponents();
    }

    public @NonNull ItemStack mutableCopy() {
        return stack.copy();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ImmutableItemStack that = (ImmutableItemStack) o;
        return ItemStack.isSameItemSameComponents(this.stack, that.stack) && this.getCount() == that.getCount();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getItem(), getComponents(), getCount());
    }

    public static Optional<ImmutableItemStack> parse(HolderLookup.Provider provider, Tag nbt) {
        return ItemStack.parse(provider, nbt).map(ImmutableItemStack::new);
    }
}
