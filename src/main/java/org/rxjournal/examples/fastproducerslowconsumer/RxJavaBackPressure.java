package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.rxjournal.util.DSUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sample program to show how RxJava deals with back pressure
 */
public class RxJavaBackPressure {

    public static void main(String[] args) {
        run(BackpressureStrategy.BUFFER);
        run(BackpressureStrategy.LATEST);
        run(BackpressureStrategy.DROP);
        run(BackpressureStrategy.MISSING);
        run(BackpressureStrategy.ERROR);
    }

    private static void run(BackpressureStrategy backpressureStrategy) {
        System.out.println("RUNNING WITH [" + backpressureStrategy + "]");
        Flowable<Long> marketDataFlowable = Flowable.create(emitter -> {
            AtomicLong publishedCount = new AtomicLong(0);
            while (true) {
                publishedCount.incrementAndGet();
                if (publishedCount.get() == 500) {
                    emitter.onComplete();
                    break;
                }
                DSUtil.sleep(5);
                emitter.onNext(publishedCount.get());
            }
        }, backpressureStrategy);

        AtomicInteger countReceived = new AtomicInteger(0);
        marketDataFlowable.observeOn(Schedulers.io()).subscribe(next -> {
                    countReceived.incrementAndGet();
                    if (countReceived.get() % 100 == 0) {
                        System.out.println("Received [" + countReceived.get() + "] items. Published item[" + next + "]");
                    }

                    DSUtil.sleep(10);
                },
                e -> System.out.println(backpressureStrategy + " " + e),
                () -> System.out.println(backpressureStrategy + " complete")
        );
        DSUtil.sleep(3000);
    }
}
