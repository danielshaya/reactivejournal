package org.rxjournal.examples.helloworld;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import org.junit.Assert;
import org.junit.Test;
import org.rxjournal.impl.*;
import org.rxjournal.impl.PlayOptions.Replay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  A Junit test class to test BytesToWordsProcessor
 */
public class HelloWorldTest {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldTest.class.getName());
    private static final Replay REPLAY_STRATEGY = Replay.FAST;

    @Test
    public void testHelloWorld() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxJournal rxJournal = new RxJournal(HelloWorldApp_JounalAsObserver.FILE_NAME);

        //Get the input from the recorder
        RxPlayer rxPlayer = rxJournal.createRxPlayer();
        PlayOptions options= new PlayOptions().filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER).replayStrategy(REPLAY_STRATEGY);
        ConnectableObservable<Byte> observableInput = rxPlayer.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Observable<String> observableOutput = bytesToWords.process(observableInput);

        //Send the output stream to the recorder to be validated against the recorded output
        RxValidator rxValidator = rxJournal.createRxValidator();
        Observable<ValidationResult> results = rxValidator.validate(HelloWorldApp_JounalAsObserver.FILE_NAME,
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
