package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.observables.ConnectableObservable;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.RxPlayer;
import org.rxjournal.util.DSUtil;

import java.io.IOException;

/**
 * Created by daniel on 05/05/17.
 */
public class FastProducerSlowConsumer_Playback {
    public static void main(String[] args) throws IOException {
        DSUtil.exitAfter(10_000);

        RxJournal rxJournal = new RxJournal("src/main/java/org/rxrecorder/examples/fastproducerslowconsumer/resources");
        rxJournal.writeToFile("/tmp/fcsp.txt", true);

        //Get the input from the recorder
        RxPlayer rxPlayer = rxJournal.createRxPlayer();
        PlayOptions options = new PlayOptions()
                .filter("MKT1")
                .completeAtEndOfFile(true);
        ConnectableObservable<MarketData> observableInput = rxPlayer.play(options).publish();

        SlowConsumerObserver slowMarketDataConsumer = new SlowConsumerObserver("MKT1", 500);
        observableInput.subscribe(slowMarketDataConsumer);

        observableInput.connect();
    }

}
