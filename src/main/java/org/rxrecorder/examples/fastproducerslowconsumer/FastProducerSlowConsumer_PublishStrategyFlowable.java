package org.rxrecorder.examples.fastproducerslowconsumer;

import io.reactivex.processors.PublishProcessor;
import org.reactivestreams.Processor;

import java.io.IOException;

/**
 * PublishSubject with a Flowable will throw an exception - MissingBackpressureException
 */
public class FastProducerSlowConsumer_PublishStrategyFlowable {

    public static void main(String[] args) throws IOException {
        SlowConsumerSubscriber slowMarketDataConsumer = new SlowConsumerSubscriber("MKT1", 100);

        Processor<MarketData, MarketData> marketDataSubject = PublishProcessor.create();
        FastProducer marketDataFastProducer = new FastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(1);

        //Back pressure causes the program to fail if PublisherSubject
        //marketDataSubject.observeOn(Schedulers.io()).subscribe(slowMarketDataConsumer);
        marketDataSubject.subscribe(slowMarketDataConsumer);
    }
}
