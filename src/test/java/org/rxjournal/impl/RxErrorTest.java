package org.rxjournal.impl;

import io.reactivex.*;
import io.reactivex.observables.ConnectableObservable;
import org.junit.Test;

import java.io.IOException;


/**
 * Created by daniel on 17/05/17.
 */
public class RxErrorTest {
    @Test
    public void errorTest() throws IOException{
        //publish a couple of items then an eeror
        //make sure the error is received onError()

        //try a couple of filters one with an error and one without
        //one should end with onError and one with onComplete()
        //Create the rxRecorder and delete any previous content by clearing the cache
        Flowable<String> errorFlowable = Flowable.create(
                e -> {
                    e.onNext("one");
                    e.onNext("two");
                    e.onError(new RuntimeException("Test Error"));
                },
                BackpressureStrategy.BUFFER
        );

        RxJournal rxJournal = new RxJournal("/tmp/testError");
        rxJournal.clearCache();

        //Pass the input stream into the rxRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        RxRecorder rxRecorder = rxJournal.createRxRecorder();
        rxRecorder.recordAsync(errorFlowable, "input");

        //Retrieve a stream of
        RxPlayer rxPlayer = rxJournal.createRxPlayer();
        PlayOptions options = new PlayOptions().filter("input");
        Observable recordedObservable = rxPlayer.play(options);

        //Pass the output stream (of words) into the rxRecorder which will subscribe to it and record all events.
        recordedObservable.subscribe(System.out::println,
                System.out::println,
                ()->System.out.println("Test complete"));
        //Only start the recording now because we want to make sure that the BytesToWordsProcessor and the rxRecorder
        //are both setup up to receive subscriptions.
        //Sometimes useful to see the recording written to a file
        rxJournal.writeToFile("/tmp/testError/error.txt",true);


    }
}
