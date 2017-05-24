package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.rxjournal.util.DSUtil;

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
        System.out.println("RUNNING WITH [" + backpressureStrategy + "]");
        Flowable<Long> fastConsumer = FastProducerSlowConsumer.createFastConsumer(backpressureStrategy);

        Consumer<Long> onNextSlowConsumer = FastProducerSlowConsumer.createOnNextSlowConsumer(10);
        fastConsumer.observeOn(Schedulers.io()).subscribe(onNextSlowConsumer::accept,
                e -> System.out.println(backpressureStrategy + " " + e),
                () -> System.out.println(backpressureStrategy + " complete")
        );
        DSUtil.sleep(3000);
    }
}
