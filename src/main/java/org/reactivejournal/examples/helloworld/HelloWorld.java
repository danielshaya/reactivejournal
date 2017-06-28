package org.reactivejournal.examples.helloworld;

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
        //1. Create the reactiveRecorder and delete any previous content by clearing the cache
        ReactiveJournal reactiveJournal = new ReactiveJournal(tmpDir + File.separator + "HW");
        reactiveJournal.clearCache();

        //2. Create a HelloWorld Publisher
        Publisher<String> helloWorldFlowable = subscriber -> {
            subscriber.onNext("Hello World!");
            subscriber.onComplete();
        };

        //3. Pass the Publisher into the reactiveRecorder which will subscribe to it and record all events.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createReactiveRecorder();
        reactiveRecorder.record(helloWorldFlowable, "");

        //4. Subscribe to ReactiveJournal and print out results
        Publisher recordedObservable = reactiveJournal.createReactivePlayer().play(new PlayOptions());

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

        //5. Sometimes useful to see the recording written to a file
        reactiveJournal.writeToFile(tmpDir + File.separator + "/hw.txt",true);
    }
}
