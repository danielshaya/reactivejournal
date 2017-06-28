package org.reactivejournal.examples.helloworld;

import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.ReactiveRecorder;
import org.reactivejournal.impl.rxjava.RxJavaPlayer;
import org.reactivejournal.util.DSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Class to run BytesToWordsProcessor
 */
public class HelloWorldApp_JournalPlayThrough {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldApp_JournalPlayThrough.class.getName());

    public final static String FILE_NAME = System.getProperty("filename", "/tmp/Demo");
    public final static int INTERVAL_MS = 10;
    public final static String INPUT_FILTER = "input";
    public final static String OUTPUT_FILTER = "output";
    public static final Flowable observableInput =
            Flowable.fromArray(new Byte[]{72,101,108,108,111,32,87,111,114,108,100,32}).map(
                    i->{
                        DSUtil.sleep(INTERVAL_MS);
                        return i;
                    });

    public static void main(String[] args) throws IOException {

        //Create the reactiveRecorder and delete any previous content by clearing the cache
        ReactiveJournal reactiveJournal = new ReactiveJournal(FILE_NAME);
        reactiveJournal.clearCache();

        //Pass the input stream into the reactiveRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createReactiveRecorder();
        reactiveRecorder.recordAsync(observableInput, INPUT_FILTER);

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();

        //Retrieve a stream of
        RxJavaPlayer rxPlayer = new RxJavaPlayer(reactiveJournal);
        PlayOptions options = new PlayOptions().filter(INPUT_FILTER).playFromNow(true);
        ConnectableFlowable recordedObservable = rxPlayer.play(options).publish();
        //Pass the input Byte stream into the BytesToWordsProcessor class which subscribes to the stream and returns
        //a stream of words.
        Flowable<String> flowableOutput = bytesToWords.process(recordedObservable);

        //Pass the output stream (of words) into the reactiveRecorder which will subscribe to it and record all events.
        reactiveRecorder.record(flowableOutput, OUTPUT_FILTER);
        flowableOutput.subscribe(s -> LOG.info("HelloWorldHot->" + s),
                throwable -> LOG.error("", throwable),
                ()->LOG.info("HelloWorldHot Complete"));
        //Only start the recording now because we want to make sure that the BytesToWordsProcessor and the reactiveRecorder
        //are both setup up to receive subscriptions.
        recordedObservable.connect();
        //Sometimes useful to see the recording written to a file
        reactiveJournal.writeToFile("/tmp/Demo/demo.txt",true);
    }
}
