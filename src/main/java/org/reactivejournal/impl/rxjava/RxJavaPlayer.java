package org.reactivejournal.impl.rxjava;

import io.reactivex.Flowable;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.ReactivePlayer;

/**
 * A specific RxJava implementation of ReactivePlayer.
 */
public class RxJavaPlayer {
    private ReactivePlayer reactivePlayer;

    public RxJavaPlayer(ReactiveJournal reactiveJournal){
        reactivePlayer = reactiveJournal.createRxPlayer();
    }

    public Flowable play(PlayOptions options){
        return Flowable.fromPublisher(reactivePlayer.play(options));
    }
}
