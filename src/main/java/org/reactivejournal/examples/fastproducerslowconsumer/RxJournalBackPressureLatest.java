package org.reactivejournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.ReactiveRecorder;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;
import org.reactivejournal.util.DSUtil;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Program to demonstrate how ReactiveJournal handles back pressure if you want LATEST rather than Buffer.
 */
public class RxJournalBackPressureLatest {

    public static void main(String[] args) throws IOException {

        ReactiveJournal reactiveJournal = new ReactiveJournal("/tmp/fastproducer");
        reactiveJournal.clearCache();
        Flowable<Long> fastProducer = FastProducerSlowConsumer.createFastProducer(BackpressureStrategy.MISSING, 2500);

        ReactiveRecorder recorder = reactiveJournal.createReactiveRecorder();
        recorder.recordAsync(fastProducer,"input");
        //Set the replay strategy to ReplayRate.FAST as e want to process the event as soon as it is
        //received from the publisher.
        PlayOptions options = new PlayOptions().filter("input").replayRate(PlayOptions.ReplayRate.FAST);
        ConnectableFlowable journalInput = new RxJavaPlayer(reactiveJournal).play(options).publish();

        Consumer onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(10);

        recorder.record(journalInput, "consumed");

        long startTime = System.currentTimeMillis();
        journalInput.observeOn(Schedulers.io()).subscribe(onNextSlowConsumer::accept,
                e -> System.out.println("ReactiveRecorder " + " " + e),
                () -> System.out.println("ReactiveRecorder complete [" + (System.currentTimeMillis()-startTime) + "]")
        );

        journalInput.connect();

        DSUtil.sleep(3000);
    }
}
