package org.reactivejournal.examples.helloworld;

import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;
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
        ReactiveJournal reactiveJournal = new ReactiveJournal(HelloWorldApp_JounalAsObserver.FILE_NAME);
        //Get the input from the remote process
        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        PlayOptions options = new PlayOptions().filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER)
                .playFromNow(true).replayRate(PlayOptions.ReplayRate.FAST);
        ConnectableFlowable<Byte> remoteInput = rxPlayer.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Flowable<String> flowableOutput = bytesToWords.process(remoteInput);


        flowableOutput.subscribe(
                s->LOG.info("Remote input [{}]", s),
                e-> LOG.error("Problem in remote [{}]", e),
                ()->{
                    LOG.info("Remote input ended");
                    System.exit(0);
                });

        remoteInput.connect();
    }
}
