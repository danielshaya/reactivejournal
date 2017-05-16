package org.rxjournal.examples.helloworld;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import org.rxjournal.impl.PlayOptions;
import org.rxjournal.impl.RxJournal;
import org.rxjournal.impl.RxPlayer;
import org.rxjournal.impl.RxRecorder;
import org.rxjournal.util.DSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Class to run BytesToWordsProcessor
 */
public class HelloWorldApp {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldApp.class.getName());

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

        //Create the rxRecorder and delete any previous content by clearing the cache
        RxJournal rxJournal = new RxJournal(FILE_NAME);
        rxJournal.clearCache();

        //Pass the input stream into the rxRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        RxRecorder rxRecorder = rxJournal.createRxRecorder();
        rxRecorder.recordAsync(observableInput, INPUT_FILTER);

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();

        //Retrieve a stream of
        RxPlayer rxPlayer = rxJournal.createRxPlayer();
        PlayOptions options = new PlayOptions().filter(INPUT_FILTER).playFromNow(true);
        ConnectableObservable recordedObservable = rxPlayer.play(options).publish();
        //Pass the input Byte stream into the BytesToWordsProcessor class which subscribes to the stream and returns
        //a stream of words.
        Observable<String> observableOutput = bytesToWords.process(recordedObservable);

        //Pass the output stream (of words) into the rxRecorder which will subscribe to it and record all events.
        rxRecorder.record(observableOutput, OUTPUT_FILTER);
        observableOutput.subscribe(s -> LOG.info("HelloWorldHot->" + s),
                throwable -> LOG.error("", throwable),
                ()->LOG.info("HelloWorldHot Complete"));
        //Only start the recording now because we want to make sure that the BytesToWordsProcessor and the rxRecorder
        //are both setup up to receive subscriptions.
        recordedObservable.connect();
        //Sometimes useful to see the recording written to a file
        rxJournal.writeToFile("/tmp/Demo/demo.txt",true);
    }
}
