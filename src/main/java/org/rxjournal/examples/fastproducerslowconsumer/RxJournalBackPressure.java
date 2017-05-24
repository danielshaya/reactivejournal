package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Program to demonstrate how RxJournal handles back pressure.
 */
public class RxJournalBackPressure {

    public static void main(String[] args) throws IOException {

        RxJournal rxJournal = new RxJournal("/tmp/fastproducer");
        rxJournal.clearCache();
        Flowable<Long> fastConsumer = FastProducerSlowConsumer.createFastConsumer(BackpressureStrategy.MISSING);

        rxJournal.createRxRecorder().recordAsync(fastConsumer,"input");
        PlayOptions options = new PlayOptions().filter("input").replayStrategy(PlayOptions.Replay.FAST);
        Observable journalInput = rxJournal.createRxPlayer().play(options);

        Consumer onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(10);

        long startTime = System.currentTimeMillis();
        journalInput.subscribe(onNextSlowConsumer::accept,
                e -> System.out.println("RxRecorder " + " " + e),
                () -> System.out.println("RxRecorder complete [" + (System.currentTimeMillis()-startTime) + "]")
        );

    }
}
