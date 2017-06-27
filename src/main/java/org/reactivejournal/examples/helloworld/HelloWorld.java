package org.reactivejournal.examples.helloworld;

import io.reactivex.Flowable;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.ReactiveRecorder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.File;
import java.io.IOException;

/**
 * Simple Demo Program
 */
public class HelloWorld {
    private static final String tmpDir = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) throws IOException {
        //Create the reactiveRecorder and delete any previous content by clearing the cache
        ReactiveJournal reactiveJournal = new ReactiveJournal(tmpDir + File.separator + "HW");
        reactiveJournal.clearCache();

        //Create a HelloWorld Publisher
        Publisher<String> helloWorldFlowable = subscriber -> {
            subscriber.onNext("Hello World!");
            subscriber.onComplete();
        };

        //Pass the Publisher into the reactiveRecorder which will subscribe to it and record all events.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createRxRecorder();
        reactiveRecorder.record(helloWorldFlowable, "");

        //Create a Publisher by subscribing to ReactiveJournal
        Publisher recordedObservable = reactiveJournal.createRxPlayer().play(new PlayOptions());

        recordedObservable.subscribe(new Subscriber() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object o) {
                System.out.println(o);
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println(throwable);
            }

            @Override
            public void onComplete() {
                System.out.println("Hello World Complete");
            }
        });

        //Sometimes useful to see the recording written to a file
        reactiveJournal.writeToFile("/tmp/Demo/demo.txt",true);
    }
}
