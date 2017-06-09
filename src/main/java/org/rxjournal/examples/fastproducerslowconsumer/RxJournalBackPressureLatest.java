package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.RxRecorder;
import org.rxjournal.impl.rxjava.RxJavaPlayer;
import org.rxjournal.util.DSUtil;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Program to demonstrate how RxJournal handles back pressure if you want LATEST rather than Buffer.
 */
public class RxJournalBackPressureLatest {

    public static void main(String[] args) throws IOException {

        RxJournal rxJournal = new RxJournal("/tmp/fastproducer");
        rxJournal.clearCache();
        Flowable<Long> fastProducer = FastProducerSlowConsumer.createFastProducer(BackpressureStrategy.MISSING, 2500);

        RxRecorder recorder = rxJournal.createRxRecorder();
        recorder.recordAsync(fastProducer,"input");
        //Set the replay strategy to ReplayRate.FAST as e want to process the event as soon as it is
        //received from the publisher.
        PlayOptions options = new PlayOptions().filter("input").replayRate(PlayOptions.ReplayRate.FAST);
        ConnectableFlowable journalInput = new RxJavaPlayer(rxJournal).play(options).publish();

        Consumer onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(10);

        recorder.record(journalInput, "consumed");

        long startTime = System.currentTimeMillis();
        journalInput.observeOn(Schedulers.io()).subscribe(onNextSlowConsumer::accept,
                e -> System.out.println("RxRecorder " + " " + e),
                () -> System.out.println("RxRecorder complete [" + (System.currentTimeMillis()-startTime) + "]")
        );

        journalInput.connect();

        DSUtil.sleep(3000);
    }
}
