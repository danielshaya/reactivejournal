package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;

import java.io.IOException;

/**
 * Created by daniel on 07/12/16.
 */
public class FastProducerSlowConsumer_ReplayStrategy {

    public static void main(String[] args) throws IOException {
        SlowConsumerObserver slowMarketDataConsumer = new SlowConsumerObserver("MKT1", 1000);

        Subject<MarketData> marketDataSubject = ReplaySubject.create();
        FastProducer marketDataFastProducer = new FastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(Integer.MIN_VALUE);

        //Back pressure causes the program to fail if PublisherSubject
        marketDataSubject.observeOn(Schedulers.io()).subscribe(slowMarketDataConsumer);
    }
}
