package li.cil.oc.api.prefab;

import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public abstract class ComponentConnectableRackMountableEnvironment extends ComponentConnectableManagedEnvironment implements RackMountable {
	@Override
	public int getConnectableCount() {
		return 0;
	}

	@Override
	public RackBusConnectable getConnectableAt(int index) {
		return null;
	}

	@Override
	public boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
		return false;
	}

	@Override
	public EnumSet<State> getCurrentState() {
		return EnumSet.noneOf(State.class);
	}
}
