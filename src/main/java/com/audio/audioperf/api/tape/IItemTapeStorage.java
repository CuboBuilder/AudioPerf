package com.audio.audioperf.api.tape;

import net.minecraft.world.item.ItemStack;

public interface IItemTapeStorage {
    ITapeStorage getStorage(ItemStack stack);
    int getSize(ItemStack stack);
}