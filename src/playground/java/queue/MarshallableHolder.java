package queue;

import net.openhft.chronicle.wire.Marshallable;

/**
 * Created by daniel on 27/04/17.
 */
public class MarshallableHolder implements Marshallable {
    Object held;

    public MarshallableHolder(Object held) {
        this.held = held;
    }

    @Override
    public String toString() {
        return "MarshallableHolder{" +
                "held=" + held +
                '}';
    }
}
