package org.reactivejournal.impl;

import io.reactivex.internal.util.BackpressureHelper;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.ValueIn;
import org.reactivejournal.impl.PlayOptions.ReplayRate;
import org.reactivejournal.util.DSUtil;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class to playback data recorded into ReactiveJournal.
 */
public class ReactivePlayer<T> {
    private ReactiveJournal reactiveJournal;

    ReactivePlayer(ReactiveJournal reactiveJournal) {
        this.reactiveJournal = reactiveJournal;
    }

    /**
     * See documentation on {@link PlayOptions}
     *
     * @param options Options controlling how play is executed.
     */
    public Publisher<T> play(PlayOptions options) {
        options.validate();

        return new PlayPublisher(reactiveJournal, options);
    }

    final class PlayPublisher implements Publisher<T> {

        private final ReactiveJournal reactiveJournal;
        private final PlayOptions options;

        PlayPublisher(ReactiveJournal reactiveJournal, PlayOptions options) {
            this.reactiveJournal = reactiveJournal;
            this.options = options;
        }

        @Override
        public void subscribe(Subscriber<? super T> s) {
            s.onSubscribe(new PlaySubscription(s, reactiveJournal, options));
        }
    }

    private final class PlaySubscription implements Subscription {
        private final AtomicLong requested = new AtomicLong(0);
        private ChronicleQueue queue;
        private Subscriber<? super T> subscriber;
        private PlayOptions options;
        private volatile boolean cancelled = false;
        private final ExcerptTailer tailer;
        private final DataItemProcessor dim;
        private ExecutorService executorService;

        PlaySubscription(Subscriber<? super T> subscriber, ReactiveJournal journal, PlayOptions options) {
            this.queue = journal.createQueue();
            this.subscriber = subscriber;
            this.options = options;
            this.tailer = queue.createTailer();
            this.dim = new DataItemProcessor();
        }

        @Override
        public void request(long n) {
            if (!options.sameThread()) {
                if(executorService == null) {
                    executorService = Executors.newSingleThreadExecutor(runnable -> {
                        Thread thread = new Thread(runnable);
                        thread.setDaemon(true);
                        thread.setName("Subscription Runner [" + System.currentTimeMillis() + "]");
                        return thread;
                    });
                    executorService.submit(() -> {
                        while (true) {
                            if (drain()) {
                                return;
                            }
                        }
                    });
                }
                BackpressureHelper.add(requested, n);
            } else {
                if (n > 0) {
                    if (BackpressureHelper.add(requested, n) == 0) {
                        drain();
                    }
                }
            }
        }


        @Override
        public void cancel() {
            cancelled = true;
        }

        /*
         * Returns whether the subscription has terminated or not
         */
        boolean drain() {
            //System.out.println("1 enter drain");
            long[] lastTime = new long[]{Long.MIN_VALUE};


            while (requested.get() != 0) { // don't emit more than requested
                if (cancelled) {
                    return true;
                }

                boolean empty = nextItemFromQueue(tailer, options, dim);
                //System.out.println("Next item from was empty? " + empty);
                if (empty) {
                    if (options.completeAtEndOfFile()) {
                        subscriber.onComplete();
                        //System.out.println("Complete at end of file");
                        return true;
                    }
                    //wait for next event to appear in the journal
                    continue;
                }

                if (!itemMatchingFilter(options, dim)) {
                    //System.out.println("Item did not match filter");
                    //wait for next matching event to appear in the journal
                    continue;
                }

                //System.out.println("About to pause for actual time");
                if (options.replayRate() == ReplayRate.ACTUAL_TIME) {
                    pauseForActualTime(lastTime, dim);
                }
                //System.out.println("After to pause for actual time");

                //is there an error
                if (dim.getStatus() == ReactiveStatus.ERROR) {
                    subscriber.onError((Throwable) dim.getObject());
                    return true;
                }

                //is the request complete
                if (isComplete(options, dim)) {
                    subscriber.onComplete();
                    return true;
                }

                //System.out.println("4 returning object " + dim.getObject() + " requested " + requested);
                subscriber.onNext((T) dim.getObject());
                requested.decrementAndGet();
            }
            return false;
            // if a concurrent getAndIncrement() happened, we loop back and continue
        }

    }

    private boolean isComplete(PlayOptions options, DataItemProcessor dim) {
        return dim.getStatus() == ReactiveStatus.COMPLETE
                || dim.getTime() > options.playUntilTime()
                || dim.getMessageCount() >= options.playUntilSeqNo();
    }

    private boolean itemMatchingFilter(PlayOptions options, DataItemProcessor dim) {
        return options.filter().equals(dim.getFilter())
                && !(dim.getTime() < options.playFromTime()
                || dim.getMessageCount() < options.playFromSeqNo());

    }

    private void pauseForActualTime(long[] lastTime, DataItemProcessor dim) {
        if (lastTime[0] != Long.MIN_VALUE) {
            DSUtil.sleep((int) (dim.getTime() - lastTime[0]));
        }
        lastTime[0] = dim.getTime();
    }

    /*
     * @return Whether the queue is empty or not
     */
    private boolean nextItemFromQueue(ExcerptTailer tailer,
                                      PlayOptions options, DataItemProcessor dim) {
        return !tailer.readDocument(w -> {
            ValueIn in = w.getValueIn();
            dim.process(in, options.using());
        });
    }
}