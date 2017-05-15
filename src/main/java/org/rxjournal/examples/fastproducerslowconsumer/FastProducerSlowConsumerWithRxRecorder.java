package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.util.DSUtil;

import java.io.IOException;

/**
 * Created by daniel on 07/12/16.
 */
public class FastProducerSlowConsumerWithRxRecorder {

    private static String file = "/tmp/MarketData";

    public static void main(String[] args) throws IOException {
        DSUtil.exitAfter(10_000);
        System.setProperty("chronicle.queueBlockSize", "1");

        RxJournal rxJournal = new RxJournal(file);
        rxJournal.clearCache();

        SlowConsumerObserver slowMarketDataConsumer = new SlowConsumerObserver("MKT1", 1000);

        Subject<MarketData> marketDataSubject = PublishSubject.create();
        FastProducer marketDataFastProducer = new FastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(1);
        rxJournal.createRxRecorder().recordAsync(marketDataSubject,"MKT1");

        PlayOptions options = new PlayOptions().filter("MKT1");
        rxJournal.createRxPlayer().play(options).subscribe(slowMarketDataConsumer);
    }
}
