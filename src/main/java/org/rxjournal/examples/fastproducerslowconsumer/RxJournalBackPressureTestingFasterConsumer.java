package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.RxPlayer;
import org.rxjournal.util.DSUtil;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Created by daniel on 05/05/17.
 */
public class RxJournalBackPressureTestingFasterConsumer {
    public static void main(String[] args) throws IOException {
        RxJournal rxJournal = new RxJournal("src/main/java/org/rxjournal/examples/fastproducerslowconsumer/resources/");
        rxJournal.writeToFile("/tmp/fcsp.txt", true);

        //Get the input from the recorder
        PlayOptions options = new PlayOptions()
                .filter("input")
                .replayStrategy(PlayOptions.Replay.FAST)
                .completeAtEndOfFile(true);
        Observable journalInput = rxJournal.createRxPlayer().play(options);

        Consumer onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(5);

        long startTime = System.currentTimeMillis();
        journalInput.subscribe(onNextSlowConsumer::accept,
                e -> System.out.println("RxRecorder " + " " + e),
                () -> System.out.println("RxRecorder complete [" + (System.currentTimeMillis()-startTime) + "]")
        );
    }

}
