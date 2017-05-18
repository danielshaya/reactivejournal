package org.rxjournal.examples.helloworld;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.RxPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 */
public class HelloWorldRemote {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldRemote.class.getName());

    public static void main(String... args) throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxJournal rxJournal = new RxJournal(HelloWorldApp_JounalAsObserver.FILE_NAME);
        //Get the input from the remote process
        RxPlayer rxPlayer = rxJournal.createRxPlayer();
        PlayOptions options = new PlayOptions().filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER).playFromNow(true);
        ConnectableObservable<Byte> remoteInput = rxPlayer.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Observable<String> observableOutput = bytesToWords.process(remoteInput);


        observableOutput.subscribe(
                s->LOG.info("Remote input [{}]", s),
                e-> LOG.error("Problem in remote [{}]", e),
                ()->{
                    LOG.info("Remote input ended");
                    System.exit(0);
                });

        remoteInput.connect();
    }
}
