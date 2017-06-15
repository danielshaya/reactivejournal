package org.reactivejournal.impl;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.junit.Assert;
import org.junit.Test;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;
import org.reactivejournal.util.DSUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by daniel on 17/05/17.
 */
public class ReactiveFromUntilTimeTest {
    @Test
    public void fromUntilTest() throws IOException {
        long[] time = new long[1];
        Flowable<String> errorFlowable = Flowable.create(
                e -> {
                    time[0] = System.currentTimeMillis();
                    e.onNext("one");
                    DSUtil.sleep(1000);
                    e.onNext("two");
                    DSUtil.sleep(200);
                    e.onNext("three");
                    e.onComplete();
                },
                BackpressureStrategy.BUFFER
        );


        ReactiveJournal reactiveJournal = new ReactiveJournal("/tmp/testFromUntil");
        reactiveJournal.writeToFile("/tmp/testError/error.txt", true);
        reactiveJournal.clearCache();

        //Pass the input stream into the reactiveRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createRxRecorder();
        reactiveRecorder.recordAsync(errorFlowable, "fromuntil");

        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        PlayOptions options = new PlayOptions().filter("fromuntil").sameThreadMaxRequests(true);
        Flowable recordedObservable = rxPlayer.play(options);

        AtomicInteger onNext = new AtomicInteger(0);
        AtomicInteger onComplete = new AtomicInteger(0);
        AtomicInteger onError = new AtomicInteger(0);

        //Pass the output stream (of words) into the reactiveRecorder which will subscribe to it and record all events.
        recordedObservable.subscribe(i -> onNext.incrementAndGet(),
                e -> {
                    onError.incrementAndGet();
                },
                () -> onComplete.incrementAndGet());
        Assert.assertEquals(3, onNext.get());
        Assert.assertEquals(0, onError.get());
        Assert.assertEquals(1, onComplete.get());


        rxPlayer = new RxJavaPlayer(reactiveJournal);
        options = new PlayOptions().filter("fromuntil")
                .replayRate(PlayOptions.ReplayRate.FAST)
                .playFromTime(time[0] + 200)
                .playUntilTime(time[0] + 1100)
                .sameThreadMaxRequests(true);
        recordedObservable = rxPlayer.play(options);

        AtomicInteger onNextFromUntil = new AtomicInteger(0);
        AtomicInteger onCompleteFromUntil = new AtomicInteger(0);
        AtomicInteger onErrorFromUntil = new AtomicInteger(0);

        String[] result = new String[1];
        recordedObservable.subscribe(i -> {
                    onNextFromUntil.incrementAndGet();
                    result[0] = (String) i;
                },
                e -> {
                    onErrorFromUntil.incrementAndGet();
                },
                () -> onCompleteFromUntil.incrementAndGet());

        Assert.assertEquals(1, onNextFromUntil.get());
        Assert.assertEquals(0, onErrorFromUntil.get());
        Assert.assertEquals(1, onCompleteFromUntil.get());
        Assert.assertEquals("two", result[0]);


        reactiveJournal.writeToFile("/tmp/testError/error.txt", true);

    }
}
