package org.rxjournal.impl.rxjava;

import io.reactivex.Flowable;
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

    public Flowable play(PlayOptions options){
        return Flowable.fromPublisher(rxPlayer.play(options));
    }
}
