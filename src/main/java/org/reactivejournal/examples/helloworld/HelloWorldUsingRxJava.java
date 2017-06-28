package org.reactivejournal.examples.helloworld;

import io.reactivex.Flowable;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.ReactiveRecorder;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;

import java.io.File;
import java.io.IOException;

/**
 * Simple Demo Program using ReactiveJournal integrated with RXJava
 */
public class HelloWorldUsingRxJava {
    private static final String tmpDir = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) throws IOException {
        //1. Create the reactiveRecorder and delete any previous content by clearing the cache
        ReactiveJournal reactiveJournal = new ReactiveJournal(tmpDir + File.separator + "HW");
        reactiveJournal.clearCache();

        //2. Create an RxJava Hello World Flowable
        Flowable<String> helloWorldFlowable = Flowable.just("Hello World!!");
        //3. Pass the flowable into the reactiveRecorder which will subscribe to it and record all events.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createReactiveRecorder();
        reactiveRecorder.record(helloWorldFlowable, "");

        //4. Use the utility class RxJavaPlayer to subscribe to the journal
        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        Flowable recordedObservable = rxPlayer.play(new PlayOptions());

        //5. Print out what we get as a callback to onNext().
        recordedObservable.subscribe(System.out::println);

        //6. Sometimes useful to see the recording written to a file
        reactiveJournal.writeToFile(tmpDir + File.separator + "/hw.txt",true);
    }
}
