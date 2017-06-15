package org.reactivejournal.impl;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.junit.Assert;
import org.junit.Test;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;

import java.io.IOException;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by daniel on 17/05/17.
 */
public class ReactiveErrorTest {
    @Test
    public void errorTest() throws IOException{
        //publish a couple of items then an eeror
        //make sure the error is received onError()

        //try a couple of filters one with an error and one without
        //one should end with onError and one with onComplete()
        //Create the reactiveRecorder and delete any previous content by clearing the cache
        Throwable rte = new RuntimeException("Test Error");
        Flowable<String> errorFlowable = Flowable.create(
                e -> {
                    e.onNext("one");
                    e.onNext("two");
                    e.onError(rte);
                },
                BackpressureStrategy.BUFFER
        );

        Flowable<Integer> validFlowable = Flowable.create(
                e -> {
                    e.onNext(100);
                    e.onNext(200);
                    e.onComplete();
                },
                BackpressureStrategy.BUFFER
        );

        ReactiveJournal reactiveJournal = new ReactiveJournal("/tmp/testError");
        reactiveJournal.writeToFile("/tmp/testError/error.txt",true, ZoneId.systemDefault());
        reactiveJournal.clearCache();

        //Pass the input stream into the reactiveRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createRxRecorder();
        reactiveRecorder.recordAsync(errorFlowable, "errorinput");
        reactiveRecorder.recordAsync(validFlowable, "validinput");

        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        PlayOptions options = new PlayOptions().filter("errorinput").sameThreadMaxRequests(true);
        Flowable recordedObservable = rxPlayer.play(options);

        AtomicInteger onNext = new AtomicInteger(0);
        AtomicInteger onComplete = new AtomicInteger(0);
        AtomicInteger onError = new AtomicInteger(0);
        Throwable[] tArray = new Throwable[1];

        //Pass the output stream (of words) into the reactiveRecorder which will subscribe to it and record all events.
        recordedObservable.subscribe(i->onNext.incrementAndGet(),
                e->{
                    onError.incrementAndGet();
                    tArray[0] = (Throwable) e;
                },
                ()->onComplete.incrementAndGet());

        Assert.assertEquals(2, onNext.get());
        Assert.assertEquals(1, onError.get());
        Assert.assertEquals(0, onComplete.get());
        Assert.assertEquals(rte.getMessage(), tArray[0].getMessage());
        Assert.assertEquals(rte.getClass(), tArray[0].getClass());
        Assert.assertEquals(rte.getStackTrace()[0], tArray[0].getStackTrace()[0]);

        rxPlayer = new RxJavaPlayer(reactiveJournal);
        options = new PlayOptions().filter("validinput").sameThreadMaxRequests(true);
        recordedObservable = rxPlayer.play(options);

        AtomicInteger onNextValid = new AtomicInteger(0);
        AtomicInteger onCompleteValid = new AtomicInteger(0);
        AtomicInteger onErrorValid = new AtomicInteger(0);

        //Pass the output stream (of words) into the reactiveRecorder which will subscribe to it and record all events.
        recordedObservable.subscribe(i->onNextValid.incrementAndGet(),
                e->{
                    onErrorValid.incrementAndGet();
                },
                ()->onCompleteValid.incrementAndGet());

        Assert.assertEquals(2, onNextValid.get());
        Assert.assertEquals(0, onErrorValid.get());
        Assert.assertEquals(1, onCompleteValid.get());

        reactiveJournal.writeToFile("/tmp/testError/error.txt",true);

    }
}
