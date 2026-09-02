package li.cil.oc.api.datacomponents;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static net.minecraft.core.component.DataComponents.CUSTOM_DATA;

public class NbtComponentHolder implements DataComponentHolder {
    protected static final String COMPONENTS_TAG = "opencomputers:components";

    public static boolean hasComponents(@NonNull CompoundTag tag) {
        return tag.get(COMPONENTS_TAG) instanceof CompoundTag;
    }

    protected final PatchedDataComponentMap components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
    private final @Nullable CompoundTag tag;

    protected NbtComponentHolder() {
        tag = null;
    }

    public NbtComponentHolder(@NonNull CompoundTag tag, HolderLookup.@NonNull Provider provider) {
        this.tag = tag;

        if (tag.get(COMPONENTS_TAG) instanceof CompoundTag componentTag) {
            DataComponentPatch.CODEC
                .parse(provider.createSerializationContext(NbtOps.INSTANCE), componentTag)
                .resultOrPartial()
                .ifPresent(components::applyPatch);
        }

        // Keep the complete legacy tag available to data-component migrators.
        components.set(CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public @Nullable <T> T get(@NotNull DataComponentType<? extends T> component) {
        if (tag == null || component != CUSTOM_DATA || components.has(CUSTOM_DATA)) {
            return DataComponentHolder.super.get(component);
        } else {
            // this is safe, as we know the component type is CUSTOM_DATA
            // which must be DataComponentType<CustomData>, making <T> = <CustomData>
            //noinspection unchecked
            return (T) components.set(CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public @NonNull DataComponentMap getComponents() {
        return components;
    }
}
