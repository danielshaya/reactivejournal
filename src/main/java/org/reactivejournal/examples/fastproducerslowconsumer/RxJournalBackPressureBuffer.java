package org.reactivejournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Program to demonstrate how ReactiveJournal handles back pressure.
 */
public class RxJournalBackPressureBuffer {

    public static void main(String[] args) throws IOException {

        ReactiveJournal reactiveJournal = new ReactiveJournal("/tmp/fastproducer");
        reactiveJournal.clearCache();
        Flowable<Long> fastProducer = FastProducerSlowConsumer.createFastProducer(BackpressureStrategy.MISSING, 500);

        reactiveJournal.createRxRecorder().recordAsync(fastProducer,"input");
        PlayOptions options = new PlayOptions().filter("input").replayRate(PlayOptions.ReplayRate.FAST);
        Flowable journalInput = new RxJavaPlayer(reactiveJournal).play(options);

        Consumer onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(10);

        long startTime = System.currentTimeMillis();
        journalInput.subscribe(onNextSlowConsumer::accept,
                e -> System.out.println("ReactiveRecorder " + " " + e),
                () -> System.out.println("ReactiveRecorder complete [" + (System.currentTimeMillis()-startTime) + "]")
        );

    }
}
