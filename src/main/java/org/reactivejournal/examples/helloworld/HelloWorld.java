package org.reactivejournal.examples.helloworld;

import io.reactivex.Flowable;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.ReactiveRecorder;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;

import java.io.IOException;

/**
 * Simple Demo Program
 */
public class HelloWorld {
    public static void main(String[] args) throws IOException {
        //Create the reactiveRecorder and delete any previous content by clearing the cache
        ReactiveJournal reactiveJournal = new ReactiveJournal(System.getProperty("filename", "/tmp/Demo"));
        reactiveJournal.clearCache();

        Flowable<String> helloWorldFlowable = Flowable.just("Hello World!!");
        //Pass the flowable into the reactiveRecorder which will subscribe to it and record all events.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createRxRecorder();
        reactiveRecorder.record(helloWorldFlowable, "");

        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        Flowable recordedObservable = rxPlayer.play(new PlayOptions());

        recordedObservable.subscribe(System.out::println);

        //Sometimes useful to see the recording written to a file
        reactiveJournal.writeToFile("/tmp/Demo/demo.txt",true);
    }
}
