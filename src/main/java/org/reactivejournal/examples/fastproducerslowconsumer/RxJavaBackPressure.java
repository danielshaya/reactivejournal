package org.reactivejournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.reactivejournal.util.DSUtil;

import java.util.function.Consumer;

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
        System.out.println("-- RUNNING WITH [" + backpressureStrategy + "] --");
        Flowable<Long> fastProducer = FastProducerSlowConsumer.createFastProducer(backpressureStrategy, 1000);

        Consumer<Long> onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(5);
        fastProducer.observeOn(Schedulers.io()).subscribe(onNextSlowConsumer::accept,
                e -> System.out.println(backpressureStrategy + " " + e),
                () -> System.out.println(backpressureStrategy + " complete")
        );
        if(backpressureStrategy== BackpressureStrategy.BUFFER)DSUtil.sleep(5000);
        DSUtil.sleep(1000);
    }
}
