package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.rxjava.RxJavaPlayer;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Program to demonstrate how RxJournal handles back pressure.
 */
public class RxJournalBackPressureBuffer {

    public static void main(String[] args) throws IOException {

        RxJournal rxJournal = new RxJournal("/tmp/fastproducer");
        rxJournal.clearCache();
        Flowable<Long> fastProducer = FastProducerSlowConsumer.createFastProducer(BackpressureStrategy.MISSING, 500);

        rxJournal.createRxRecorder().recordAsync(fastProducer,"input");
        PlayOptions options = new PlayOptions().filter("input").replayRate(PlayOptions.ReplayRate.FAST);
        Observable journalInput = new RxJavaPlayer(rxJournal).play(options);

        Consumer onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(10);

        long startTime = System.currentTimeMillis();
        journalInput.subscribe(onNextSlowConsumer::accept,
                e -> System.out.println("RxRecorder " + " " + e),
                () -> System.out.println("RxRecorder complete [" + (System.currentTimeMillis()-startTime) + "]")
        );

    }
}
