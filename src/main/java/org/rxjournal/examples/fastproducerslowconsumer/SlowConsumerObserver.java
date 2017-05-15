package org.rxjournal.examples.fastproducerslowconsumer;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.rxjournal.util.DSUtil;

/**
 * Created by daniel on 10/04/17.
 */
public class SlowConsumerObserver implements Observer<MarketData> {
    private String id;
    private int delayMS;
    private int count;

    public SlowConsumerObserver(String id, int delayMS) {
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
    public void onSubscribe(Disposable disposable) {
        System.out.println(id + ": SlowConsumerObserver subscribes");
    }

    @Override
    public void onNext(MarketData marketData) {
        DSUtil.sleep(delayMS);
        System.out.println(++count + ":" + id + ": SlowConsumerObserver consumed " + marketData);
    }
}
