package org.rxjournal.impl;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.observables.ConnectableObservable;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.rxjournal.examples.helloworld.BytesToWordsProcessor;
import org.rxjournal.examples.helloworld.HelloWorldApp_JounalAsObserver;
import org.rxjournal.impl.PlayOptions.ReplayRate;
import org.rxjournal.impl.rxjava.RxJavaPlayer;
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
    private static final ReplayRate REPLAY_RATE_STRATEGY = ReplayRate.FAST;
    private final String tmpDir = System.getProperty("java.io.tmpdir");


    @Test
    public void testPlay() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxJournal rxJournal = new RxJournal("src/test/resources/");
        //rxJournal.writeToFile(tmpDir +"/rctext.txt", true);

        //Get the input from the recorder
        RxJavaPlayer rxPlayer = new RxJavaPlayer(rxJournal);
        PlayOptions options= new PlayOptions()
                .filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER)
                .replayRate(REPLAY_RATE_STRATEGY)
                .completeAtEndOfFile(false);
        ConnectableObservable<Byte> observableInput = rxPlayer.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Flowable<String> flowableOutput = bytesToWords.process(observableInput.toFlowable(BackpressureStrategy.BUFFER));

        CountDownLatch latch = new CountDownLatch(1);
        //Send the output stream to the recorder to be validated against the recorded output
        RxValidator rxValidator = rxJournal.createRxValidator();
        rxValidator.validate("src/test/resources/",
                flowableOutput, HelloWorldApp_JounalAsObserver.OUTPUT_FILTER, new Subscriber() {
                    @Override
                    public void onSubscribe(Subscription subscription) {

                    }

                    @Override
                    public void onNext(Object o) {
                        LOG.info(o.toString());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOG.error("Problem in process test [{}]", throwable);
                    }

                    @Override
                    public void onComplete() {
                        LOG.info("Summary[" + rxValidator.getValidationResult().summaryResult()
                                + "] items compared[" + rxValidator.getValidationResult().summaryItemsCompared()
                                + "] items valid[" + rxValidator.getValidationResult().summaryItemsValid() +"]");
                        latch.countDown();
                    }
                });

        observableInput.connect();
        boolean completedWithoutTimeout = latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(ValidationResult.Result.OK, rxValidator.getValidationResult().getResult());
        Assert.assertTrue(completedWithoutTimeout);
    }
}
