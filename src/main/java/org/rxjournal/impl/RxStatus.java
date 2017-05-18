package org.rxjournal.impl;

/**
 * Created by daniel on 18/05/17.
 */
public interface RxStatus {
    byte VALID = 0;
    byte ERROR = 1;
    byte COMPLETE = 2;

    static String toString(byte status){
        switch (status){
            case VALID:
                return "VALID";
            case ERROR:
                return "ERROR";
            case COMPLETE:
                return "COMPLETE";
            default:
                return "UNKNOWN";
        }
    }
}
