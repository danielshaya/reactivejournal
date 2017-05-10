package org.rxrecorder.examples.fastproducerslowconsumer;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.rxrecorder.util.DSUtil;

/**
 * Created by daniel on 10/04/17.
 */
public class SlowConsumerSubscriber implements Subscriber<MarketData> {
    private String id;
    private int delayMS;
    private int count;

    public SlowConsumerSubscriber(String id, int delayMS) {
        this.id = id;
        this.delayMS = delayMS;
    }

    @Override
    public void onComplete() {
        System.out.println(id + ": SlowConsumerObserver completes");
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        System.out.println(id + ": SlowConsumerObserver subscribes");
    }

    @Override
    public void onNext(MarketData marketData) {
        DSUtil.sleep(delayMS);
        System.out.println(++count + ":" + id + ": SlowConsumerObserver consumed " + marketData);
    }
}
