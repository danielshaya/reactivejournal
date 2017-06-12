package org.reactivejournal.examples.helloworld;

import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.ReactiveRecorder;
import org.reactivejournal.util.DSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * An example class that demonstrates how use rxRecorder in with a cold Observable.
 * i.e. The Observable will only produce events in response to a subscribe.
 */
public class HelloWorldApp_JounalAsObserver {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldApp_JounalAsObserver.class.getName());

    public final static String FILE_NAME = System.getProperty("filename", "/tmp/Demo");
    public final static int INTERVAL_MS = 100;
    public final static String INPUT_FILTER = "input";
    public final static String OUTPUT_FILTER = "output";

    public static void main(String[] args) throws IOException {
        ConnectableFlowable flowableInput =
                Flowable.fromArray(new Byte[]{72,101,108,108,111,32,87,111,114,108,100,32}).map(
                        i->{
                            DSUtil.sleep(INTERVAL_MS);
                            return i;
                        }).publish();

        //Create the reactiveRecorder and delete any previous content by clearing the cache
        ReactiveJournal reactiveJournal = new ReactiveJournal(FILE_NAME);
        reactiveJournal.clearCache();

        //Pass the input stream into the reactiveRecorder which will subscribe to it and record all events.
        //The subscription will not be activated until 'connect' is called on the input stream.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createRxRecorder();
        reactiveRecorder.record(flowableInput, INPUT_FILTER);

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        //Pass the input Byte stream into the BytesToWordsProcessor class which subscribes to the stream and returns
        //a stream of words.
        //The subscription will not be activated until 'connect' is called on the input stream.
        Flowable<String> flowableOutput = bytesToWords.process(flowableInput);

        //Pass the output stream (of words) into the reactiveRecorder which will subscribe to it and record all events.
        flowableOutput.subscribe(LOG::info);
        reactiveRecorder.record(flowableOutput, OUTPUT_FILTER);

        //Activate the subscriptions
        flowableInput.connect();

        //Sometimes useful to see the recording written to a file
        reactiveJournal.writeToFile("/tmp/Demo/demo.txt",true);
    }
}
