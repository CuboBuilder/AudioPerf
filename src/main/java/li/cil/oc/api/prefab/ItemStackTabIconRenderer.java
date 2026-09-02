package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Simple implementation of a tab icon renderer using an item stack as its graphic.
 * In 1.18, this class isn't required
 */
@SuppressWarnings("UnusedDeclaration")
@Deprecated
public class ItemStackTabIconRenderer implements TabIconRenderer {
    private final ItemStack stack;

    public ItemStackTabIconRenderer(ItemStack stack) {
        this.stack = stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics) {
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(
                net.minecraft.client.Minecraft.getInstance().font,
                stack,
                0,
                0
        );
    }
}