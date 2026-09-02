package li.cil.oc.api;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Immutable wrapper around {@link FluidStack} for use in Minecraft data components.
 *
 * FluidStack itself is mutable and therefore must not be stored directly as a data
 * component value.
 */
public final class ImmutableFluidStack {
    private final @NonNull FluidStack stack;

    public static final Codec<ImmutableFluidStack> CODEC =
            FluidStack.OPTIONAL_CODEC.xmap(ImmutableFluidStack::new, ImmutableFluidStack::getStackUnsafe);

    public static final ImmutableFluidStack EMPTY = new ImmutableFluidStack(FluidStack.EMPTY);

    private ImmutableFluidStack(@NonNull FluidStack stack) {
        this.stack = stack.copy();
    }

    private FluidStack getStackUnsafe() {
        // Codec serialization only reads the stack, and the wrapper itself never exposes
        // this instance to callers.
        return stack;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NonNull ImmutableFluidStack copyOf(@NonNull FluidStack stack) {
        return new ImmutableFluidStack(stack);
    }

    @Contract(pure = true)
    public @NonNull FluidStack mutableCopy() {
        return stack.copy();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableFluidStack that)) return false;
        return FluidStack.matches(this.stack, that.stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(FluidStack.hashFluidAndComponents(stack), stack.getAmount());
    }
}
