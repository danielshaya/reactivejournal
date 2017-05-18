package org.rxjournal.impl;

import net.openhft.chronicle.wire.ValueIn;

/**
 * Created by daniel on 18/05/17.
 */
public class DataItemProcessor {

    private byte status;
    private long messageCount;
    private long time;
    private String storedFilter;
    private Object valueFromQueue;

    public void process(ValueIn in, Object using){
        status = in.int8();
        messageCount = in.int64();
        time = in.int64();
        storedFilter = in.text();

        if(status == RxStatus.ERROR){
            valueFromQueue = in.throwable(false);
        }else {
            if(using== null) {
                valueFromQueue = in.object();
            }else{
                valueFromQueue = in.object(using, using.getClass());
            }
        }
    }

    public byte getStatus() {
        return status;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public long getTime() {
        return time;
    }

    public String getFilter() {
        return storedFilter;
    }

    public Object getObject() {
        return valueFromQueue;
    }
}
