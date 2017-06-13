package org.reactivejournal.impl;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Test cases for request
 */
public class ReactiveRequestTest {
    @Test
    public void testRequest() throws IOException{
        test(10, 1,1);
        test(100, 1000,1);
        test(10, 1,3);
    }

    private void test(int messageCount, int initialRequest, int onNextRequest) throws IOException {
        CountDownLatch latch = new CountDownLatch(1);

        Flowable<String> errorFlowable = Flowable.create(
                e -> {
                    for (int i = 0; i < messageCount; i++) {
                        e.onNext("" + i);
                    }
                    e.onComplete();
                },
                BackpressureStrategy.BUFFER
        );


        ReactiveJournal reactiveJournal = new ReactiveJournal("/tmp/testRequest");
        reactiveJournal.clearCache();

        //Pass the input stream into the reactiveRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createRxRecorder();
        reactiveRecorder.recordAsync(errorFlowable, "request");

        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        PlayOptions options = new PlayOptions().filter("request");
        Flowable recordedObservable = rxPlayer.play(options);

        AtomicInteger onNext = new AtomicInteger(0);
        AtomicInteger onComplete = new AtomicInteger(0);
        AtomicInteger onError = new AtomicInteger(0);

        List results = new ArrayList();
        //Pass the output stream (of words) into the reactiveRecorder which will subscribe to it and record all events.
        recordedObservable.subscribe(new Subscriber() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(initialRequest);
            }

            @Override
            public void onNext(Object o) {
                onNext.incrementAndGet();
                results.add(o);
                subscription.request(onNextRequest);
            }

            @Override
            public void onError(Throwable throwable) {
                onError.incrementAndGet();
            }

            @Override
            public void onComplete() {
                onComplete.incrementAndGet();
                latch.countDown();
            }

        });


        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(messageCount, onNext.get());
        Assert.assertEquals(0, onError.get());
        Assert.assertEquals(1, onComplete.get());

        for(int i=0; i<messageCount; i++){
            Assert.assertEquals(""+i, results.get(i));
        }

    }

    @Test
    public void testCancel() throws IOException {
        int messageCount = 10;
        int initialRequest = 1;
        int onNextRequest = 1;

        CountDownLatch latch = new CountDownLatch(1);

        Flowable<String> errorFlowable = Flowable.create(
                e -> {
                    for (int i = 0; i < messageCount; i++) {
                        e.onNext("" + i);
                    }
                    e.onComplete();
                },
                BackpressureStrategy.BUFFER
        );


        ReactiveJournal reactiveJournal = new ReactiveJournal("/tmp/testRequest");
        reactiveJournal.clearCache();

        //Pass the input stream into the reactiveRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createRxRecorder();
        reactiveRecorder.recordAsync(errorFlowable, "request");

        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        PlayOptions options = new PlayOptions().filter("request");
        Flowable recordedObservable = rxPlayer.play(options);

        AtomicInteger onNext = new AtomicInteger(0);
        AtomicInteger onComplete = new AtomicInteger(0);
        AtomicInteger onError = new AtomicInteger(0);

        List results = new ArrayList();
        //Pass the output stream (of words) into the reactiveRecorder which will subscribe to it and record all events.
        recordedObservable.subscribe(new Subscriber() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(initialRequest);
            }

            @Override
            public void onNext(Object o) {
                onNext.incrementAndGet();
                results.add(o);
                subscription.request(onNextRequest);
                subscription.cancel();
            }

            @Override
            public void onError(Throwable throwable) {
                onError.incrementAndGet();
            }

            @Override
            public void onComplete() {
                onComplete.incrementAndGet();
                latch.countDown();
            }

        });



        try {
            boolean await = latch.await(500, TimeUnit.MILLISECONDS);
            Assert.assertFalse(await);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(1, onNext.get());
        Assert.assertEquals(0, onError.get());
        Assert.assertEquals(0, onComplete.get());

        for(int i=0; i<1; i++){
            Assert.assertEquals(""+i, results.get(i));
        }

    }
}
