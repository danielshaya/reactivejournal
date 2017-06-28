package org.reactivejournal.impl;

import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import org.junit.Assert;
import org.junit.Test;
import org.reactivejournal.examples.helloworld.BytesToWordsProcessor;
import org.reactivejournal.examples.helloworld.HelloWorldApp_JournalPlayThrough;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  Test for RxRecorder
 */
public class ReactiveRecorderTest {
    private final String tmpDir = System.getProperty("java.io.tmpdir");

    /*
     * Test the record and play functionality as well as the writeToFile.
     *
     * Record to a journal, play from it into the BytesProcessor and then write the data to a file.
     *
     * Compare the data in the file with a file produced from a control run.
     */
    @Test
    public void recorderTest() throws IOException{
        //Flowable used to create the control run.
        Flowable<Byte> observableInput = HelloWorldApp_JournalPlayThrough.observableInput;

        //Create the rxRecorder and delete any previous content by clearing the cache
        ReactiveJournal reactiveJournal = new ReactiveJournal(tmpDir +"/playTest");
        reactiveJournal.clearCache();

        //Pass the input stream into the rxRecorder which will subscribe to it and record all events.
        //The subscription will happen on a new thread which will allow this program to continue.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createReactiveRecorder();
        reactiveRecorder.recordAsync(observableInput, "input");

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();

        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        PlayOptions options = new PlayOptions().filter("input").playFromNow(true).sameThread(true);
        ConnectableFlowable recordedObservable = rxPlayer.play(options).publish();
        //Pass the input Byte stream into the BytesToWordsProcessor class which subscribes to the stream and returns
        //a stream of words.
        Flowable<String> flowableOutput = bytesToWords.process(recordedObservable);

        //Pass the output stream (of words) into the rxRecorder which will subscribe to it and record all events.
        reactiveRecorder.record(flowableOutput, "output");

        //Only start the recording now because we want to make sure that the BytesToWordsProcessor and the rxRecorder
        //are both setup up to receive subscriptions.
        recordedObservable.connect();
        reactiveJournal.writeToFile(tmpDir + "/playTest/playTest.txt",true);

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
        return parts[3] + "\t" + parts[4];
    }
}
