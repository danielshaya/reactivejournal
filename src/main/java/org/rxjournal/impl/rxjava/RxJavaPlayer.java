package org.rxjournal.impl.rxjava;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.RxPlayer;

/**
 * A specific RxJava implementation of RxPlayer.
 */
public class RxJavaPlayer {
    private RxPlayer rxPlayer;

    public RxJavaPlayer(RxJournal rxJournal){
        rxPlayer = rxJournal.createRxPlayer();
    }

    public Observable play(PlayOptions options){
       return Observable.create(subscriber->{
           rxPlayer.play(new Subscriber() {
               @Override
               public void onSubscribe(Subscription subscription) {
               }

               @Override
               public void onNext(Object o) {
                    subscriber.onNext(o);
               }

               @Override
               public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
               }

               @Override
               public void onComplete() {
                    subscriber.onComplete();
               }
           },options);
       });
    }

    public Flowable play(PlayOptions options, BackpressureStrategy backpressureStrategy){
        return Flowable.create(subscriber->{
            rxPlayer.play(new Subscriber() {
                @Override
                public void onSubscribe(Subscription subscription) {
                }

                @Override
                public void onNext(Object o) {
                    subscriber.onNext(o);
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            },options);
        }, backpressureStrategy);
    }
}
