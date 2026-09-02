package li.cil.oc.api.event;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * A bit more specific sign change event that holds information about new text of the sign. Used in the sign upgrade.
 */
public abstract class SignChangeEvent extends Event {
    public final SignBlockEntity sign;
    public final String[] lines;

    private SignChangeEvent(SignBlockEntity sign, String[] lines) {
        this.sign = sign;
        this.lines = lines;
    }

    public static class Pre extends SignChangeEvent implements ICancellableEvent {
        public Pre(SignBlockEntity sign, String[] lines) {
            super(sign, lines);
        }
    }

    public static class Post extends SignChangeEvent {
        public Post(SignBlockEntity sign, String[] lines) {
            super(sign, lines);
        }
    }
}
