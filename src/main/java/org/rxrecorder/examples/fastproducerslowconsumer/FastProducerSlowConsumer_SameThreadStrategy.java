package org.rxrecorder.examples.fastproducerslowconsumer;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.rxrecorder.util.DSUtil;

import java.io.IOException;

/**
 * The problem with this strategy is that it slows down the producer!
 */
public class FastProducerSlowConsumer_SameThreadStrategy {

    public static void main(String[] args) throws IOException {
        DSUtil.exitAfter(5000);

        SlowConsumer slowMarketDataConsumer = new SlowConsumer("MKT1", 1000);

        Subject<MarketData> marketDataSubject = PublishSubject.create();
        FastProducer marketDataFastProducer = new FastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(1, true);

        //Back pressure causes the program to fail if PublisherSubject
        marketDataSubject.subscribe(slowMarketDataConsumer);
    }
}
