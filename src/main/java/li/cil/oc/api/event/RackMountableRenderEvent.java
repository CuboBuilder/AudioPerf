package li.cil.oc.api.event;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.internal.Rack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired to allow rendering a custom overlay for {@link RackMountable}s.
 * <br>
 * When this event is fired, the GL state is set up such that the origin is
 * the top left corner of the mountable the event was fired for. It's the
 * event handler's responsibility to not render outside the are of the
 * mountable (unless that's explicitly what they're going for, of course).
 */
public abstract class RackMountableRenderEvent extends Event {
    /**
     * The rack that house the mountable this event is fired for.
     */
    public final Rack rack;

    /**
     * The index of the mountable in the rack the event is fired for.
     */
    public final int mountable;

    /**
     * Some additional data made available by the mountable. May be {@code null}.
     *
     * @see RackMountable#describeForClient
     */
    public final DataComponentHolder data;

    public RackMountableRenderEvent(Rack rack, int mountable, DataComponentHolder data) {
        this.rack = rack;
        this.mountable = mountable;
        this.data = data;
    }

    /**
     * Fired when the static rack model is rendered.
     * <br>
     * Code here runs as a part of model baking, so functionality is
     * limited to what's possible with models. This is meant to allow
     * setting a custom front texture for the mountables front.
     * <br>
     * The bounds will be set up before this call, so you may adjust those, if you wish.
     */
    public static class Block extends RackMountableRenderEvent implements ICancellableEvent {
        /**
         * The front-facing side, i.e. where the mountable is visible on the rack.
         */
        public final Direction side;

        /**
         * Texture to use for the front of the mountable.
         */
        private TextureAtlasSprite frontTextureOverride;

        public Block(final Rack rack, final int mountable, final DataComponentHolder data, final Direction side) {
            super(rack, mountable, data);
            this.side = side;
        }

        /**
         * The texture currently set to use for the front of the mountable, or {@code null}.
         */
        public TextureAtlasSprite getFrontTextureOverride() {
            return frontTextureOverride;
        }

        /**
         * Set the texture to use for the front of the mountable.
         *
         * @param texture the texture to use.
         */
        public void setFrontTextureOverride(final TextureAtlasSprite texture) {
            frontTextureOverride = texture;
        }
    }

    /**
     * Fired when the dynamic rack model is rendered.
     * <br>
     * Code here runs inside a {@link BlockEntityRenderer}, so go nuts. This is
     * primarily meant to allow rendering custom overlays, such as LEDs. The GL state
     * will have been adjusted such that rendering a one by one quad starting at the
     * origin will fill the full front face of the rack (i.e. rotation and translation
     * have already been applied).
     */
    public static class BlockEntity extends RackMountableRenderEvent {
        /**
         * The transformation used by the rendering engine.
         */
        public final PoseStack stack;

        /**
         * An accessor to the renderer's buffer context.
         */
        public final MultiBufferSource typeBuffer;

        /**
         * Packed block light and overlay texture coordinates.
         */
        public final int light, overlay;

        /**
         * The vertical low and high texture coordinates for the mountable's slot.
         * <br>
         * This is purely for convenience; they're computed as {@code (2/16)+i*(3/16)}.
         */
        public final float v0, v1;

        public BlockEntity(final Rack rack, final int mountable, final DataComponentHolder data, final PoseStack stack, final MultiBufferSource typeBuffer, final int light, final int overlay, final float v0, final float v1) {
            super(rack, mountable, data);
            this.stack = stack;
            this.typeBuffer = typeBuffer;
            this.light = light;
            this.overlay = overlay;
            this.v0 = v0;
            this.v1 = v1;
        }
    }
}
