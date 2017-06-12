package org.reactivejournal.examples.helloworld;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple BytesToWordsProcessor program. Receives a stream of Bytes and converts to
 * a stream of words. (This is intended for demonstration purposes only.)
 */
public class BytesToWordsProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BytesToWordsProcessor.class.getName());
    public Flowable<String> process(Flowable<Byte> observableInput){

        PublishProcessor<String> publishProcessor = PublishProcessor.create();

        StringBuilder sb = new StringBuilder();
        observableInput.subscribe(b->{
                if(b==32){ //send out a new word on a space
                    publishProcessor.onNext(sb.toString());
                    sb.setLength(0);
                }else{
                    sb.append((char)b.byteValue());
                }
            },
            e->LOG.error("Error in BytesToWordsProcessor [{}]", e),
            publishProcessor::onComplete
        );

        return publishProcessor;
    }
}
