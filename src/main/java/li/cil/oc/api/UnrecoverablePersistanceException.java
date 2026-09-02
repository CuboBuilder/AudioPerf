package li.cil.oc.api;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.AvailableSince("1.9; NeoForge 1.21.1+")
public class UnrecoverablePersistanceException extends Exception {
    public UnrecoverablePersistanceException(String message) {
        super(message);
    }
}
