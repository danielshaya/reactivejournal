package org.rxrecorder.impl;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import org.junit.Assert;
import org.junit.Test;
import org.rxrecorder.examples.helloworld.BytesToWordsProcessor;
import org.rxrecorder.examples.helloworld.HelloWorldAppCold;
import org.rxrecorder.util.DSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rxrecorder.impl.RxRecorder.Replay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *  Test for RxRecorder
 */
public class RxRecoderTest {
    private static final Logger LOG = LoggerFactory.getLogger(RxRecoderTest.class.getName());
    private static final Replay REPLAY_STRATEGY = Replay.FAST;
    private final String tmpDir = System.getProperty("java.io.tmpdir");

    @Test
    public void playTest() throws IOException{
        Flowable<Byte> observableInput =
                Flowable.fromArray(new Byte[]{72,101,108,108,111,32,87,111,114,108,100,32}).map(
                        i->{
                            DSUtil.sleep(10);
                            return i;
                        });

        //Create the rxRecorder and delete any previous content by clearing the cache
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(tmpDir +"/playTest", true);

        //Pass the input stream into the rxRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        rxRecorder.recordAsync(observableInput, "input");

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();

        //Retrieve a stream of
        PlayOptions options = new PlayOptions().filter("input").playFromNow(true);
        ConnectableObservable recordedObservable = rxRecorder.play(options).publish();
        //Pass the input Byte stream into the BytesToWordsProcessor class which subscribes to the stream and returns
        //a stream of words.
        Observable<String> observableOutput = bytesToWords.process(recordedObservable);

        //Pass the output stream (of words) into the rxRecorder which will subscribe to it and record all events.
        rxRecorder.record(observableOutput, "output");

        //Only start the recording now because we want to make sure that the BytesToWordsProcessor and the rxRecorder
        //are both setup up to receive subscriptions.
        recordedObservable.connect();
        rxRecorder.writeToFile(tmpDir + "/playTest/playTest.txt",true);

        List<String> toBeTested = Files.readAllLines(Paths.get(tmpDir + "/playTest/playTest.txt"));
        List<String> controlSet = Files.readAllLines(Paths.get("src/test/resources/playTest.txt"));

        Assert.assertEquals(controlSet.size(), toBeTested.size());

        //Asert all the values are in both files - they might not be in exactly the same order

        String[] controlSetInput = getFilterLinesFromFiles(controlSet, "input");
        String[] toBeTestedInput= getFilterLinesFromFiles(toBeTested, "input");
        Assert.assertArrayEquals(controlSetInput, toBeTestedInput);

        String[] controlSetOutput = getFilterLinesFromFiles(controlSet, "output");
        String[] toBeTestedOutput= getFilterLinesFromFiles(toBeTested, "output");
        Assert.assertArrayEquals(controlSetOutput, toBeTestedOutput);

        String[] controlSetEOS = getFilterLinesFromFiles(controlSet, "endOfStream");
        String[] toBeTestedEOS= getFilterLinesFromFiles(toBeTested, "endOfStream");
        Assert.assertArrayEquals(controlSetEOS, toBeTestedEOS);
    }

    private String[] getFilterLinesFromFiles(List<String> lines, String filter){
         return lines.stream()
                .map(this::removeTimeStampAndMessageCount)
                .filter(s->s.contains(filter))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    private String removeTimeStampAndMessageCount(String line){
         String[] parts = line.split("\\t");
         return parts[2] + "\t" + parts[3];
    }

    @Test
    public void testPlayback() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init("src/test/resources/", false);
        rxRecorder.writeToFile(tmpDir +"/rctext.txt", true);

        //Get the input from the recorder
        PlayOptions options= new PlayOptions()
                .filter(HelloWorldAppCold.INPUT_FILTER)
                .replayStrategy(REPLAY_STRATEGY)
                .completeAtEndOfFile(false);
        ConnectableObservable<Byte> observableInput = rxRecorder.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Observable<String> observableOutput = bytesToWords.process(observableInput);

        //Send the output stream to the recorder to be validated against the recorded output
        RxValidator rxValidator = new RxValidator();
        Observable<ValidationResult> results = rxValidator.validate("src/test/resources/",
                observableOutput, HelloWorldAppCold.OUTPUT_FILTER);

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
