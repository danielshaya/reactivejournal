package org.reactivejournal.impl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.ValueIn;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivejournal.impl.PlayOptions.PauseStrategy;
import org.reactivejournal.impl.PlayOptions.ReplayRate;
import org.reactivejournal.util.DSUtil;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Class to playback data recorded into ReactiveJournal.
 */
public class ReactivePlayer {
    private ReactiveJournal reactiveJournal;

    ReactivePlayer(ReactiveJournal reactiveJournal) {
        this.reactiveJournal = reactiveJournal;
    }

    /**
     * See documentation on {@link PlayOptions}
     * @param options Options controlling how play is executed.
     */
    public Publisher<Object> play(PlayOptions options) {
        options.validate();

        return new PlayPublisher(reactiveJournal, options);
    }

    static final class PlayPublisher implements Publisher<Object> {

        private final ReactiveJournal reactiveJournal;
        private final PlayOptions options;

        PlayPublisher(ReactiveJournal reactiveJournal, PlayOptions options) {
            this.reactiveJournal = reactiveJournal;
            this.options = options;
        }

        @Override
        public void subscribe(Subscriber<? super Object> s) {
            s.onSubscribe(new PlaySubscription(s, reactiveJournal, options));
        }
    }

    static final class PlaySubscription implements Subscription {

        private final Subscriber actual;
        private final ReactiveJournal reactiveJournal;
        private final PlayOptions options;
        private final DataItemProcessor dim = new DataItemProcessor();

        private volatile boolean cancelled;
        private final ExcerptTailer tailer;
        private final AtomicLong counter = new AtomicLong(0);
        private volatile boolean executing = false;

        PlaySubscription(Subscriber<? super Object> actual, ReactiveJournal journal, PlayOptions options) {
            this.actual = actual;
            this.reactiveJournal = journal;
            this.options = options;
            ChronicleQueue queue = reactiveJournal.createQueue();
            tailer = queue.createTailer();
        }

        @Override
        public void request(long n) {
            counter.addAndGet(n);
            if(!executing) {
                executing = true;
                execute();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        private void execute() {
                long[] lastTime = new long[]{Long.MIN_VALUE};
                boolean[] stop = new boolean[]{false};
                while (counter.get() > 0) {
                    boolean foundItem = tailer.readDocument(w -> {
                        if (cancelled) {
                            return;
                        }
                        ValueIn in = w.getValueIn();
                        dim.process(in, options.using());

                        if (dim.getTime() > options.playUntilTime()
                                || dim.getMessageCount() >= options.playUntilSeqNo()) {
                            actual.onComplete();
                            stop[0] = true;
                            return;
                        }

                        if (dim.getTime() > options.playFromTime() && dim.getMessageCount() >= options.playFromSeqNo()) {
                            pause(options, lastTime, dim.getTime());
                            if (options.filter().equals(dim.getFilter())) {
                                if (dim.getStatus() == ReactiveStatus.COMPLETE) {
                                    actual.onComplete();
                                    stop[0] = true;
                                    return;
                                }

                                if (dim.getStatus() == ReactiveStatus.ERROR) {
                                    actual.onError((Throwable) dim.getObject());
                                    stop[0] = true;
                                    return;
                                }
                                counter.decrementAndGet();
                                actual.onNext(dim.getObject());
                            }
                            lastTime[0] = dim.getTime();
                        }
                    });
                    if (cancelled) {
                        executing = false;
                        return;
                    }

                    if (!foundItem && !options.completeAtEndOfFile()) {
                        actual.onComplete();
                        executing = false;
                        return;
                    }
                    if (stop[0]) {
                        executing = false;
                        return;
                    }
                }
                executing = false;
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
}