package com.audio.audioperf;

import com.audio.audioperf.network.TapeDriveStatePayload;
import com.audio.audioperf.tile.TapeDriveMenu;
import com.audio.audioperf.tile.TapeDriveState;
import com.audio.audioperf.tile.TileTapeDrive;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class TapeDriveScreen extends AbstractContainerScreen<TapeDriveMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AudioPerf.MODID, "textures/gui/tape_player.png");

    private static final int BUTTON_START_X = 48;
    private static final int BUTTON_START_Y = 58;

    private TapeDriveState.State state = TapeDriveState.State.STOPPED;

    public TapeDriveScreen(TapeDriveMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        state = menu.getTapeDrive().getEnumState();
    }

    private boolean isButtonPressed(int buttonIndex) {
        // button order: REWIND(0), PLAY(1), STOP(2), FAST_FORWARD(3)
        switch (state) {
            case PLAYING: return buttonIndex == 1;
            case REWINDING: return buttonIndex == 0;
            case FORWARDING: return buttonIndex == 3;
            default: return false;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Draw the four transport buttons from the texture strip at (0,170)
        for (int b = 0; b < 4; b++) {
            int tx = isButtonPressed(b) ? 20 : 0;
            int ty = 170 + b * 15;
            int bx = x + BUTTON_START_X + b * 20;
            int by = y + BUTTON_START_Y;
            guiGraphics.blit(BACKGROUND, bx, by, tx, ty, 20, 15, 256, 256);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int bx0 = this.leftPos + BUTTON_START_X;
            int by0 = this.topPos + BUTTON_START_Y;
            for (int b = 0; b < 4; b++) {
                int bx = bx0 + b * 20;
                if (mouseX >= bx && mouseX < bx + 20 && mouseY >= by0 && mouseY < by0 + 15) {
                    handleButton(b);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleButton(int b) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        // 0=rewind, 1=play, 2=stop, 3=fast-forward
        TileTapeDrive td = menu.getTapeDrive();
        int targetState;
        switch (b) {
            case 0: targetState = (state == TapeDriveState.State.REWINDING) ? 0 : 2; break; // toggle rewind
            case 1: targetState = 1; break; // play
            case 2: targetState = 0; break; // stop
            case 3: targetState = (state == TapeDriveState.State.FORWARDING) ? 0 : 3; break; // toggle ffwd
            default: targetState = 0;
        }
        // Optimistically update the local state so the GUI responds immediately,
        // then let the server confirm via block entity sync.
        if (targetState >= 0 && targetState < TapeDriveState.State.VALUES.length) {
            state = TapeDriveState.State.VALUES[targetState];
        }
        PacketDistributor.sendToServer(new TapeDriveStatePayload(td.getBlockPos(), (byte) targetState));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Draw the tape label
        ItemStack stack = menu.getTapeDrive().getTapeStack();
        String label;
        int color;
        if (stack.isEmpty()) {
            label = Component.translatable("tooltip.audio_perf.tape.none").getString();
            color = 0xFF3333;
        } else {
            label = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                    .copyTag().getString("label");
            if (label.isEmpty()) {
                label = Component.translatable("tooltip.audio_perf.tape.unnamed").getString();
            }
            color = 0xFFFFFF;
        }
        if (label.length() > 22) label = label.substring(0, 20) + "...";
        guiGraphics.drawCenteredString(this.font, label, this.leftPos + 88, this.topPos + 15, color);
    }
}