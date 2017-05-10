package org.rxrecorder.examples.fastproducerslowconsumer;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.rxrecorder.impl.PlayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.util.DSUtil;

import java.io.IOException;

/**
 * Created by daniel on 07/12/16.
 */
public class FastProducerSlowConsumerWithRxRecorder {

    private static String file = "/tmp/MarketData";

    public static void main(String[] args) throws IOException {
        DSUtil.exitAfter(10_000);
        System.setProperty("chronicle.queueBlockSize", "1");

        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(file, true);

        SlowConsumerObserver slowMarketDataConsumer = new SlowConsumerObserver("MKT1", 1000);

        Subject<MarketData> marketDataSubject = PublishSubject.create();
        FastProducer marketDataFastProducer = new FastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(1);
        rxRecorder.recordAsync(marketDataSubject,"MKT1");

        PlayOptions options = new PlayOptions().filter("MKT1");
        rxRecorder.play(options).subscribe(slowMarketDataConsumer);
    }
}
