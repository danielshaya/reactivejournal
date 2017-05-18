package org.rxjournal.impl;

import io.reactivex.Emitter;
import io.reactivex.Observable;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.ValueIn;
import org.rxjournal.util.DSUtil;

/**
 * Class to playback data recorded into RxJournal.
 */
public class RxPlayer {
    private RxJournal rxJournal;

    RxPlayer(RxJournal rxJournal) {
        this.rxJournal = rxJournal;
    }

    public Observable play(PlayOptions options) {
        long fromTime = System.currentTimeMillis();

        return Observable.create(subscriber -> {
            try (ChronicleQueue queue = rxJournal.createQueue()) {
                ExcerptTailer tailer = queue.createTailer();
                long[] lastTime = new long[]{Long.MIN_VALUE};
                boolean[] stop = new boolean[]{false};
                while (true) {

                    boolean foundItem = tailer.readDocument(w -> {
                        ValueIn in = w.getValueIn();
                        byte status = in.int8();
                        long messageCount = in.int64();
                        long recordedAtTime = in.int64();
                        String storedWithFilter = in.text();

                        if (testEndOfStream(subscriber, storedWithFilter)) {
                            stop[0] = true;
                            return;
                        }

                        if (testPastPlayUntil(options, subscriber, recordedAtTime)){
                            stop[0] = true;
                            return;
                        }

                        if (options.playFrom() > recordedAtTime
                                && (!options.playFromNow() || fromTime < recordedAtTime)) {
                            pause(options, lastTime, recordedAtTime);

                            if (options.filter().equals(storedWithFilter) || RxJournal.ERROR_FILTER.equals(storedWithFilter)) {
                                if (storedWithFilter.equals(RxJournal.ERROR_FILTER)){
                                    subscriber.onError(getThrowable(in));
                                    stop[0] = true;
                                    return;
                                }
                                subscriber.onNext(getStoredObject(options, in));
                            }
                            lastTime[0] = recordedAtTime;
                        }
                    });
                    if (!foundItem && !options.completeAtEndOfFile() || stop[0]) {
                        subscriber.onComplete();
                        return;
                    }
                }
            }

        });
    }

    //todo need to do this until bug inChronicle is resolved.
    private Throwable getThrowable(ValueIn in) {
        String errorMessage = in.text();
        return new RuntimeException(errorMessage);
    }

    private boolean testPastPlayUntil(PlayOptions options, Emitter<? super Object> s, long recordedAtTime) {
        if(options.playUntil() > recordedAtTime){
            s.onComplete();
            return true;
        }
        return false;
    }

    private boolean testEndOfStream(Emitter<? super Object> s, String storedWithFilter) {
        if (storedWithFilter.equals(RxJournal.END_OF_STREAM_FILTER)) {
            s.onComplete();
            return true;
        }

        return false;
    }

    private Object getStoredObject(PlayOptions options, ValueIn in) {
        Object storedObject;
        if (options.using() != null) {
            storedObject = in.object(options.using(), options.using().getClass());
        } else {
            storedObject = in.object();
        }
        return storedObject;
    }

    private void pause(PlayOptions options, long[] lastTime, long recordedAtTime) {
        if (options.replayStrategy() == PlayOptions.Replay.REAL_TIME && lastTime[0] != Long.MIN_VALUE) {
            DSUtil.sleep((int) (recordedAtTime - lastTime[0]));
        }
        //todo add configurable pause strategy
    }
}
