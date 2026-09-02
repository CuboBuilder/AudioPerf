package li.cil.oc.api.prefab;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * Basic implementation for the {@link Value} interface.
 */
public abstract class AbstractValue implements Value {
    @Override
    public Object apply(Context context, Arguments arguments) {
        return null;
    }

    @Override
    public void unapply(Context context, Arguments arguments) {
    }

    @Override
    public Object[] call(Context context, Arguments arguments) {
        throw new RuntimeException("trying to call a non-callable value");
    }

    @Override
    public void dispose(Context context) {
    }

    @Override
    public void loadData(DataComponentHolder holder, @NonNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
    }

    @Override
    public void saveData(MutableDataComponentHolder holder, @NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
    }
}
