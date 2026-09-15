package com.audio.audioperf.api.audio;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;

/**
 * Implemented by audio blocks that carry a paint color. Cables only connect
 * to other colored audio blocks with a matching color.
 * Colors are in the common {@code 0xRRGGBB} format.
 */
public interface IAudioColored {
    /** Default (unpainted) color. White dye resets to this. */
    int DEFAULT_COLOR = 0xCCCCCC;

    int getColor();

    void setColor(int color);

    /** Maps a dye to a paint color. White dye means the default color. */
    static int dyeColor(DyeItem dye) {
        if (dye.getDyeColor() == DyeColor.WHITE) {
            return DEFAULT_COLOR;
        }
        return dye.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF;
    }
}
