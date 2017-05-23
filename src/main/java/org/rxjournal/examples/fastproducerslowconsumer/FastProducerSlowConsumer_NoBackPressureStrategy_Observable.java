package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;

import java.io.IOException;

/**
 * Created by daniel on 07/12/16.
 * Run with -Xmx64M -verbosegc
 */
public class FastProducerSlowConsumer_NoBackPressureStrategy_Observable {

    public static void main(String[] args) throws IOException {
        SlowConsumerObserver slowMarketDataConsumer = new SlowConsumerObserver("MKT1", 1000);

        Subject<MarketData> marketDataSubject = PublishSubject.create();
        FastProducer marketDataFastProducer = new FastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(Integer.MIN_VALUE);

        //The program will run out of memory as the queue backs up
        marketDataSubject.observeOn(Schedulers.io()).subscribe(slowMarketDataConsumer);
    }
}
