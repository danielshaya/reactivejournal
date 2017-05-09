package org.rxrecorder.impl;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import org.junit.Assert;
import org.junit.Test;
import org.rxrecorder.examples.helloworld.BytesToWordsProcessor;
import org.rxrecorder.examples.helloworld.HelloWorldAppCold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rxrecorder.impl.RxRecorder.Replay;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  A Junit test class to test BytesToWordsProcessor
 */
public class RxRecoderTest {
    private static final Logger LOG = LoggerFactory.getLogger(RxRecoderTest.class.getName());
    private static final Replay REPLAY_STRATEGY = Replay.FAST;

    @Test
    public void testPlayback() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init("src/test/resources/", false);
        rxRecorder.writeToFile("/tmp/rctext.txt", true);

        //Get the input from the recorder
        PlayOptions options= new PlayOptions()
                .filter(HelloWorldAppCold.INPUT_FILTER)
                .replayStrategy(REPLAY_STRATEGY)
                .completeAtEndOfFile(false);
        ConnectableObservable<Byte> observableInput = rxRecorder.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Observable<String> observableOutput = bytesToWords.process(observableInput);

        //Send the output stream to the recorder to be validated against the recorded output
        Observable<ValidationResult> results = rxRecorder.validate(observableOutput, HelloWorldAppCold.OUTPUT_FILTER);

        CountDownLatch latch = new CountDownLatch(1);
        results.subscribe(
                s->LOG.info(s.toString()),
                e-> LOG.error("Problem in process test [{}]", e),
                ()->{
                    LOG.info("Summary[" + rxRecorder.getValidationResult().summaryResult()
                            + "] items compared[" + rxRecorder.getValidationResult().summaryItemsCompared()
                            + "] items valid[" + rxRecorder.getValidationResult().summaryItemsValid() +"]");
                    latch.countDown();
                });

        observableInput.connect();
        boolean completedWithoutTimeout = latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(ValidationResult.Result.OK, rxRecorder.getValidationResult().getResult());
        Assert.assertTrue(completedWithoutTimeout);
    }
}
