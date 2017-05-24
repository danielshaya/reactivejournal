package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.rxjournal.util.DSUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Created by daniel on 24/05/17.
 */
public class FastProducerSlowConsumer {
    static Flowable<Long> createFastConsumer(BackpressureStrategy backpressureStrategy){
        return Flowable.create(emitter -> {
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
    }

    static Consumer<Long> createOnNextSlowConsumer(int delayMS){
        AtomicInteger countReceived = new AtomicInteger(0);

        return nextItem -> {
            countReceived.incrementAndGet();
            if (countReceived.get() % 100 == 0) {
                System.out.println("Received [" + countReceived.get() + "] items. Published item[" + nextItem + "]");
            }

            DSUtil.sleep(delayMS);
        };
    }
}
