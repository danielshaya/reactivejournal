package org.rxrecorder.util;

/**
 * Created by daniel on 10/05/17.
 */
@FunctionalInterface
public interface TriConsumer<A,B,C> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     */
    void accept(A a, B b, C c);
}
