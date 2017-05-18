package org.rxjournal.impl;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import org.junit.Assert;
import org.junit.Test;
import org.rxjournal.examples.helloworld.BytesToWordsProcessor;
import org.rxjournal.examples.helloworld.HelloWorldApp_JounalAsObserver;
import org.rxjournal.impl.PlayOptions.Replay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  Test for RxRecorder
 */
public class RxPlayerTest {
    private static final Logger LOG = LoggerFactory.getLogger(RxPlayerTest.class.getName());
    private static final Replay REPLAY_STRATEGY = Replay.FAST;
    private final String tmpDir = System.getProperty("java.io.tmpdir");


    @Test
    public void testPlay() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxJournal rxJournal = new RxJournal("src/test/resources/");
        //rxJournal.writeToFile(tmpDir +"/rctext.txt", true);

        //Get the input from the recorder
        RxPlayer rxPlayer = rxJournal.createRxPlayer();
        PlayOptions options= new PlayOptions()
                .filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER)
                .replayStrategy(REPLAY_STRATEGY)
                .completeAtEndOfFile(false);
        ConnectableObservable<Byte> observableInput = rxPlayer.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Observable<String> observableOutput = bytesToWords.process(observableInput);

        //Send the output stream to the recorder to be validated against the recorded output
        RxValidator rxValidator = rxJournal.createRxValidator();
        Observable<ValidationResult> results = rxValidator.validate("src/test/resources/",
                observableOutput, HelloWorldApp_JounalAsObserver.OUTPUT_FILTER);

        CountDownLatch latch = new CountDownLatch(1);
        results.subscribe(
                s->LOG.info(s.toString()),
                e-> LOG.error("Problem in process test [{}]", e),
                ()->{
                    LOG.info("Summary[" + rxValidator.getValidationResult().summaryResult()
                            + "] items compared[" + rxValidator.getValidationResult().summaryItemsCompared()
                            + "] items valid[" + rxValidator.getValidationResult().summaryItemsValid() +"]");
                    latch.countDown();
                });

        observableInput.connect();
        boolean completedWithoutTimeout = latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(ValidationResult.Result.OK, rxValidator.getValidationResult().getResult());
        Assert.assertTrue(completedWithoutTimeout);
    }
}
