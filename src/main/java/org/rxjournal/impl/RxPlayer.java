package org.rxjournal.impl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.ValueIn;
import org.reactivestreams.Subscriber;
import org.rxjournal.impl.PlayOptions.PauseStrategy;
import org.rxjournal.impl.PlayOptions.ReplayRate;
import org.rxjournal.util.DSUtil;

/**
 * Class to playback data recorded into RxJournal.
 */
public class RxPlayer {
    private RxJournal rxJournal;
    private DataItemProcessor dim = new DataItemProcessor();

    RxPlayer(RxJournal rxJournal) {
        this.rxJournal = rxJournal;
    }

    /**
     * See documentation on {@link PlayOptions}
     * @param subscriber The subscriber to be called back when events are fired
     * @param options Options controlling how play is executed.
     */
    public void play(Subscriber subscriber, PlayOptions options) {
        options.validate();

        try (ChronicleQueue queue = rxJournal.createQueue()) {
            ExcerptTailer tailer = queue.createTailer();
            long[] lastTime = new long[]{Long.MIN_VALUE};
            boolean[] stop = new boolean[]{false};
            while (true) {

                boolean foundItem = tailer.readDocument(w -> {
                    ValueIn in = w.getValueIn();
                    dim.process(in, options.using());

                    if (dim.getTime() > options.playUntilTime()
                            || dim.getMessageCount() >= options.playUntilSeqNo()) {
                        subscriber.onComplete();
                        stop[0] = true;
                        return;
                    }

                    if (dim.getTime() > options.playFromTime() && dim.getMessageCount() >= options.playFromSeqNo()) {
                        pause(options, lastTime, dim.getTime());

                        if (options.filter().equals(dim.getFilter())) {
                            if (dim.getStatus() == RxStatus.COMPLETE) {
                                subscriber.onComplete();
                                stop[0] = true;
                                return;
                            }

                            if (dim.getStatus() == RxStatus.ERROR) {
                                subscriber.onError((Throwable) dim.getObject());
                                stop[0] = true;
                                return;
                            }
                            subscriber.onNext(dim.getObject());
                        }
                        lastTime[0] = dim.getTime();
                    }
                });
                if (!foundItem && !options.completeAtEndOfFile()) {
                    subscriber.onComplete();
                    return;
                }
                if (stop[0]) {
                    return;
                }
            }
        }
    }

    private void pause(PlayOptions options, long[] lastTime, long recordedAtTime) {
        if (options.replayRate() == ReplayRate.ACTUAL_TIME && lastTime[0] != Long.MIN_VALUE) {
            DSUtil.sleep((int) (recordedAtTime - lastTime[0]));
        } else if (options.pauseStrategy() == PauseStrategy.YIELD) {
            Thread.yield();
        }
        //otherwise SPIN
    }
}
