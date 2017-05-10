package org.rxrecorder.examples.fastproducerslowconsumer;

import io.reactivex.observables.ConnectableObservable;
import org.rxrecorder.impl.PlayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.util.DSUtil;

import java.io.IOException;

/**
 * Created by daniel on 05/05/17.
 */
public class FastProducerSlowConsumer_Playback {
    public static void main(String[] args) throws IOException {
        DSUtil.exitAfter(10_000);

        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init("src/main/java/org/rxrecorder/examples/fastproducerslowconsumer/resources", false);
        rxRecorder.writeToFile("/tmp/fcsp.txt", true);

        //Get the input from the recorder
        PlayOptions options = new PlayOptions()
                .filter("MKT1")
                .completeAtEndOfFile(true);
        ConnectableObservable<MarketData> observableInput = rxRecorder.play(options).publish();

        SlowConsumerObserver slowMarketDataConsumer = new SlowConsumerObserver("MKT1", 500);
        observableInput.subscribe(slowMarketDataConsumer);

        observableInput.connect();
    }

}
