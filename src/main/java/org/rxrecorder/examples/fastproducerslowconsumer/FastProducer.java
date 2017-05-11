package org.rxrecorder.examples.fastproducerslowconsumer;

import io.reactivex.subjects.Subject;
import org.reactivestreams.Processor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to publish MarketData can be used with either Observable/Flowable.
 */
class FastProducer {
    private AtomicInteger counter = new AtomicInteger(0);
    private String id;
    private Processor<MarketData, MarketData> processor;
    private Subject<MarketData> subject;


    FastProducer(String id, Subject<MarketData> subject) {
        this.id = id;
        this.subject = subject;
    }

    FastProducer(String id, Processor<MarketData, MarketData> processor) {
        this.id = id;
        this.processor = processor;
    }

    void startPublishing(int delayMS) {
        startPublishing(delayMS, false);
    }

    void startPublishing(int delayMS, boolean logEveryItem) {
        ExecutorService scheduledExecutorService;
        if (delayMS == Integer.MIN_VALUE) {
            scheduledExecutorService = Executors.newSingleThreadExecutor();
            scheduledExecutorService.submit(() -> {
                while (true) {
                    process(logEveryItem);
                }
            });
        } else {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            ((ScheduledExecutorService) scheduledExecutorService).scheduleAtFixedRate(() ->
                    process(logEveryItem), 1, delayMS, TimeUnit.MILLISECONDS);
        }
    }

    private void process(boolean logEveryItem) {
        int count = counter.incrementAndGet();
        MarketData marketData = new MarketData(id, count, count - 0.5, count + .5);
        if(count % 1000 == 0 || logEveryItem){
            System.out.println("Published item [" + count + "] " + marketData);
        }
        if(subject!=null)
            subject.onNext(marketData);
        else
            processor.onNext(marketData);
    }


//    public void stopPublishing(){
//        LOG.info("Fast producer completing");
//        scheduledExecutorService.shutdown();
//        if(subject!=null)subject.onComplete();
//    }
}
