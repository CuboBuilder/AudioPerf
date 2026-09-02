package li.cil.oc.api.datacomponents;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MutableNbtComponentHolder extends NbtComponentHolder implements MutableDataComponentHolder {
    public MutableNbtComponentHolder() {
        super();
    }

    public MutableNbtComponentHolder(CompoundTag tag, HolderLookup.Provider provider) {
        super(tag, provider);
    }

    @Override
    public @Nullable <T> T set(@NonNull DataComponentType<? super T> componentType, @Nullable T value) {
        return components.set(componentType, value);
    }

    @Override
    public @Nullable <T> T remove(@NonNull DataComponentType<? extends T> componentType) {
        return components.remove(componentType);
    }

    @Override
    public void applyComponents(@NonNull DataComponentPatch patch) {
        components.applyPatch(patch);
    }

    @Override
    public void applyComponents(@NonNull DataComponentMap components) {
        this.components.setAll(components);
    }

    public void save(CompoundTag tag, HolderLookup.@NonNull Provider provider) {
        var encoded = DataComponentPatch.CODEC
            .encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this.components.asPatch())
            .getOrThrow();
        if (encoded instanceof CompoundTag componentTag) {
            tag.put(COMPONENTS_TAG, componentTag);
        }
    }
}
