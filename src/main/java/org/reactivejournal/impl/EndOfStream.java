package org.reactivejournal.impl;

import net.openhft.chronicle.wire.Marshallable;

/**
 * Created by daniel on 25/04/17.
 */
public class EndOfStream implements Marshallable {
    @Override
    public String toString() {
        return "EndOfStream{}";
    }
}
