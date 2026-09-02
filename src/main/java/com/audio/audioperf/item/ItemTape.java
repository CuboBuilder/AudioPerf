package com.audio.audioperf.item;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.api.tape.IItemTapeStorage;
import com.audio.audioperf.api.tape.ITapeStorage;
import com.audio.audioperf.tape.TapeStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

public class ItemTape extends Item implements IItemTapeStorage {
    public static final int L_SECOND = 4096;
    public static final int L_MINUTE = 4096 * 60;

    public static final int TAPE_COUNT = 10;
    public static final int[] DEFAULT_LENGTHS = {4, 8, 16, 32, 64, 2, 6, 16, 128, 128};

    private final int[] sizes;

    public ItemTape(Properties properties) {
        super(properties);
        this.sizes = new int[TAPE_COUNT];
        for (int i = 0; i < TAPE_COUNT; i++) {
            sizes[i] = DEFAULT_LENGTHS[i] * L_MINUTE;
        }
    }

    @Override
    public ITapeStorage getStorage(ItemStack stack) {
        int size = getSize(stack);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag.contains("storage")) {
            String storageName = tag.getString("storage");
            if (AudioPerf.instance().getStorage().exists(storageName)) {
                return AudioPerf.instance().getStorage().get(storageName, size, 0);
            }
        }
        TapeStorage storage = AudioPerf.instance().getStorage().newStorage(size);
        tag.putString("storage", storage.getUniqueId());
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return storage;
    }

    @Override
    public int getSize(ItemStack stack) {
        int index = getTapeIndex(stack);
        return sizes[Math.floorMod(index, sizes.length)];
    }

    public int getSizeMinutes(ItemStack stack) {
        return getSize(stack) / L_MINUTE;
    }

    public int getTapeIndex(ItemStack stack) {
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return cmd != null ? cmd.value() : 0;
    }

    public ItemStack withIndex(int index) {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(Math.floorMod(index, TAPE_COUNT) + 1));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        int len = getSizeMinutes(stack);
        tooltipComponents.add(Component.translatable("tooltip.audio_perf.tape.length", len)
                .withStyle(ChatFormatting.GRAY));
        String label = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("label");
        if (!label.isEmpty()) {
            tooltipComponents.add(Component.literal(label).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        }
    }
}