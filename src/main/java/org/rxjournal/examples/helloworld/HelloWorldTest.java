package org.rxjournal.examples.helloworld;

import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.RxValidator;
import org.rxjournal.impl.ValidationResult;
import org.rxjournal.impl.rxjava.RxJavaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  A demo example Junit test class to test BytesToWordsProcessor.
 *  Make sure you have run the HelloWorldApp_JounalAsObserver first to generate the journal.
 */
public class HelloWorldTest {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldTest.class.getName());

    @Test
    public void testHelloWorld() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxJournal rxJournal = new RxJournal(HelloWorldApp_JounalAsObserver.FILE_NAME);

        //Get the input from the recorder
        RxJavaPlayer rxPlayer = new RxJavaPlayer(rxJournal);
        //In this case we can play the data stream in FAST mode.
        PlayOptions options= new PlayOptions().filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER)
                .replayRate(PlayOptions.ReplayRate.FAST);
        //Use a ConnectableObservable as we only want to kick off the stream when all
        //connections have been wired together.
        ConnectableFlowable<Byte> observableInput = rxPlayer.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Flowable<String> flowableOutput = bytesToWords.process(observableInput);

        CountDownLatch latch = new CountDownLatch(1);
        //Send the output stream to the recorder to be validated against the recorded output
        RxValidator rxValidator = rxJournal.createRxValidator();
        rxValidator.validate(HelloWorldApp_JounalAsObserver.FILE_NAME,
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
