package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.rxjava.RxJavaPlayer;
import org.rxjournal.util.DSUtil;

import java.io.IOException;
import java.util.function.Consumer;

import static org.rxjournal.impl.PlayOptions.ReplayRate;

/**
 * Program to demonstrate how you can replay from the journal to test whether you have improved the
 * speed of your program.
 *
 * Using the recording created by <code>RxJournalBackPressureLatest</> we see how many more items
 * we can consume if we speed up our consumer.
 */
public class RxJournalBackPressureTestingFasterConsumer {
    public static void main(String[] args) throws IOException {
        RxJournal rxJournal = new RxJournal("src/main/java/org/rxjournal/examples/fastproducerslowconsumer/resources/");

        //Get the input from the recorder note that we have to set the replayRate to ACTUAL_TIME
        //to replicate the conditions in the 'real world'.
        PlayOptions options = new PlayOptions().filter("input").replayRate(ReplayRate.ACTUAL_TIME);
        ConnectableFlowable journalInput = new RxJavaPlayer(rxJournal).play(options).publish();

        //Reduce the latency of the consumer to 5ms - try reducing or increasing to study the effects.
        Consumer onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(3);

        long startTime = System.currentTimeMillis();
        journalInput.observeOn(Schedulers.io()).subscribe(onNextSlowConsumer::accept,
                e -> System.out.println("RxRecorder " + " " + e),
                () -> System.out.println("RxRecorder complete [" + (System.currentTimeMillis()-startTime) + "ms]")
        );

        journalInput.connect();

        DSUtil.sleep(1000);
    }

}
