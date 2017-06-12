package org.reactivejournal.impl;

import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivejournal.examples.helloworld.BytesToWordsProcessor;
import org.reactivejournal.examples.helloworld.HelloWorldApp_JounalAsObserver;
import org.reactivejournal.impl.PlayOptions.ReplayRate;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  Test for ReactiveRecorder
 */
public class ReactivePlayerTest {
    private static final Logger LOG = LoggerFactory.getLogger(ReactivePlayerTest.class.getName());
    private static final ReplayRate REPLAY_RATE_STRATEGY = ReplayRate.FAST;
    private final String tmpDir = System.getProperty("java.io.tmpdir");


    @Test
    public void testPlay() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        ReactiveJournal reactiveJournal = new ReactiveJournal("src/test/resources/", "");
        reactiveJournal.writeToFile(tmpDir +"/rctext.txt", true);

        //Get the input from the recorder
        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        PlayOptions options= new PlayOptions()
                .filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER)
                .replayRate(REPLAY_RATE_STRATEGY)
                .completeAtEndOfFile(false);
        ConnectableFlowable<Byte> observableInput = rxPlayer.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Flowable<String> flowableOutput = bytesToWords.process(observableInput);

        CountDownLatch latch = new CountDownLatch(1);
        //Send the output stream to the recorder to be validated against the recorded output
        ReactiveValidator reactiveValidator = reactiveJournal.createRxValidator();
        reactiveValidator.validate("src/test/resources/",
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
                        LOG.info("Summary[" + reactiveValidator.getValidationResult().summaryResult()
                                + "] items compared[" + reactiveValidator.getValidationResult().summaryItemsCompared()
                                + "] items valid[" + reactiveValidator.getValidationResult().summaryItemsValid() +"]");
                        latch.countDown();
                    }
                });

        observableInput.connect();
        boolean completedWithoutTimeout = latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(ValidationResult.Result.OK, reactiveValidator.getValidationResult().getResult());
        Assert.assertTrue(completedWithoutTimeout);
    }
}
