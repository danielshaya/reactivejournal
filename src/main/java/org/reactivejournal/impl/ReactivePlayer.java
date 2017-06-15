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

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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
        private final AtomicLong requests = new AtomicLong(0);
        private SubscriptionRunner subscriptionRunner;
        private ChronicleQueue queue;
        private Subscriber<? super T> subscriber;
        private PlayOptions options;
        private volatile boolean fastPathStarted = false;
        private volatile boolean cancelled = false;

        PlaySubscription(Subscriber<? super T> subscriber, ReactiveJournal journal, PlayOptions options) {
            this.queue = journal.createQueue();
            this. subscriber = subscriber;
            this.options = options;

            if(!options.sameThreadMaxRequests()) {
                subscriptionRunner = new SubscriptionRunner(subscriber, requests, queue.createTailer(), options);
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    thread.setName("Subscription Runner [" + System.currentTimeMillis() + "]");
                    return thread;
                }).submit(subscriptionRunner);
            }
        }

        @Override
        public void request(long n) {
            if(subscriptionRunner != null) {
                requests.addAndGet(n);
            } else if(!fastPathStarted){
                fastPathStarted = true;
                fastPath();
            }
            //ignore any further calls to request if we are already on the fast path
        }

        private void fastPath(){
            DataItemProcessor dim = new DataItemProcessor();
            ExcerptTailer tailer= queue.createTailer();
            boolean complete = false;
            while (!complete && !cancelled){
                complete = processNextItem(subscriber, tailer, options, dim, null);
            }
        }

        @Override
        public void cancel() {
            if(subscriptionRunner != null) {
                subscriptionRunner.setCancelled();
            }else{
                cancelled = true;
            }
        }
    }

    private long[] lastTime = new long[]{Long.MIN_VALUE};

    private boolean processNextItem(Subscriber<? super T> subscriber, ExcerptTailer tailer,
                                           PlayOptions options, DataItemProcessor dim, AtomicLong requests){
        AtomicBoolean complete = new AtomicBoolean(false);

        boolean foundItem = tailer.readDocument(w -> {
            ValueIn in = w.getValueIn();
            dim.process(in, options.using());

            if (dim.getTime() > options.playUntilTime()
                    || dim.getMessageCount() >= options.playUntilSeqNo()) {
                subscriber.onComplete();
                complete.set(true);
                return;
            }

            if (dim.getTime() > options.playFromTime() && dim.getMessageCount() >= options.playFromSeqNo()) {
                pause(options, lastTime, dim.getTime());
                if (options.filter().equals(dim.getFilter())) {
                    if (dim.getStatus() == ReactiveStatus.COMPLETE) {
                        subscriber.onComplete();
                        complete.set(true);
                        return;
                    }

                    if (dim.getStatus() == ReactiveStatus.ERROR) {
                        subscriber.onError((Throwable) dim.getObject());
                        complete.set(true);
                        return;
                    }
                    if(requests !=null){
                        requests.decrementAndGet();
                    }
                    subscriber.onNext((T)dim.getObject());
                }
                lastTime[0] = dim.getTime();
            }
        });
        if (complete.get()) {
            return true;
        }

        //todo is this correct test for end of file
        if (!foundItem && !options.completeAtEndOfFile()) {
            subscriber.onComplete();
            return true;
        }

        return false;
    }


    private static void pause(PlayOptions options, long[] lastTime, long recordedAtTime) {
        if (options.replayRate() == ReplayRate.ACTUAL_TIME && lastTime[0] != Long.MIN_VALUE) {
            DSUtil.sleep((int) (recordedAtTime - lastTime[0]));
        } else if (options.pauseStrategy() == PauseStrategy.YIELD) {
            Thread.yield();
        }
        //otherwise SPIN
    }

    private class SubscriptionRunner implements Runnable {
        private final AtomicLong requests;
        private final ExcerptTailer tailer;
        private volatile boolean cancelled = false;
        private final PlayOptions options;
        private final Subscriber<? super T> subscriber;
        private final DataItemProcessor dim = new DataItemProcessor();

        SubscriptionRunner(Subscriber<? super T> subscriber, AtomicLong counter,
                           ExcerptTailer tailer, PlayOptions options) {
            this.requests = counter;
            this.tailer = tailer;
            this.options = options;
            this.subscriber = subscriber;
        }

        void setCancelled() {
            cancelled = true;
        }

        @Override
        public void run() {
            while (true) {
                if (requests.get() > 0) {
                    if(cancelled){
                        return;
                    }
                    boolean complete = processNextItem(subscriber, tailer, options, dim, requests);
                    if(complete || cancelled){
                        return;
                    }
                }else {
                    //Wait for requests to have more requests
                    Thread.yield();
                }
            }
        }
    }
}