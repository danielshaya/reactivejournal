package org.reactivejournal.examples.fastproducerslowconsumer;

import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;
import org.reactivejournal.util.DSUtil;

import java.io.IOException;
import java.util.function.Consumer;

import static org.reactivejournal.impl.PlayOptions.ReplayRate;

/**
 * Program to demonstrate how you can replay from the journal to test whether you have improved the
 * speed of your program.
 *
 * Using the recording created by <code>RxJournalBackPressureLatest</> we see how many more items
 * we can consume if we speed up our consumer.
 */
public class RxJournalBackPressureTestingFasterConsumer {
    public static void main(String[] args) throws IOException {
        ReactiveJournal reactiveJournal = new ReactiveJournal("src/main/java/org/reactivejournal/examples/fastproducerslowconsumer/resources/");

        //Get the input from the recorder note that we have to set the replayRate to ACTUAL_TIME
        //to replicate the conditions in the 'real world'.
        PlayOptions options = new PlayOptions().filter("input").replayRate(ReplayRate.ACTUAL_TIME);
        ConnectableFlowable journalInput = new RxJavaPlayer(reactiveJournal).play(options).publish();

        //Reduce the latency of the consumer to 5ms - try reducing or increasing to study the effects.
        Consumer onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(3);

        long startTime = System.currentTimeMillis();
        journalInput.observeOn(Schedulers.io()).subscribe(onNextSlowConsumer::accept,
                e -> System.out.println("ReactiveRecorder " + " " + e),
                () -> System.out.println("ReactiveRecorder complete [" + (System.currentTimeMillis()-startTime) + "ms]")
        );

        journalInput.connect();

        DSUtil.sleep(1000);
    }

}
