package com.audio.audioperf.tile;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class TapeDriveMenu extends AbstractContainerMenu {
    private final TileTapeDrive tile;

    public TileTapeDrive getTapeDrive() {
        return tile;
    }

    public TapeDriveMenu(int id, Inventory playerInv, TileTapeDrive tile) {
        super(AudioPerfMenus.TAPE_DRIVE.get(), id);
        this.tile = tile;

        // Tape slot
        IItemHandler handler = tile.getInventory();
        addSlot(new SlotItemHandler(handler, 0, 80, 34));

        // Player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIdx) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIdx);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (slotIdx == 0) {
                if (!this.moveItemStackTo(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()).evaluate((l, p) ->
                l.getBlockEntity(p) == tile && player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= 64.0, true);
    }
}