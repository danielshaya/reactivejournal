package org.reactivejournal.impl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.WireOut;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivejournal.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * Class to record input into ReactiveJournal.
 */
public class ReactiveRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(ReactiveRecorder.class.getName());

    private ReactiveJournal reactiveJournal;

    public ReactiveRecorder(ReactiveJournal reactiveJournal) {
        this.reactiveJournal = reactiveJournal;
    }

    public void recordAsync(Publisher<?> publisher, String filter){
        new Thread(()->record(publisher,filter)).start();
    }


    private TriConsumer<ExcerptAppender, String, Object> getOnNextConsumerRecorder(){
        return (a, f, v) -> a.writeDocument(w -> {
            writeObject(w, f, v, ReactiveStatus.VALID);
        });
    }

    private BiConsumer<ExcerptAppender, String> getOnCompleteRecorder(){
        return (a,f) -> a.writeDocument(w -> {
            writeObject(w, f, new EndOfStream(), ReactiveStatus.COMPLETE);
            LOG.debug("Adding end of stream token");
        });
    }

    private TriConsumer<ExcerptAppender, String, Throwable> getOnErrorRecorder(){
        return (a, f, t) -> a.writeDocument(w -> {
            writeObject(w, f, t, ReactiveStatus.ERROR);
        });
    }

    private void writeObject(WireOut wireOut, String filter, Object obj, byte status){
        wireOut.getValueOut().int8(status);
        wireOut.getValueOut().int64(reactiveJournal.getMessageCounter().incrementAndGet());
        wireOut.getValueOut().int64(System.currentTimeMillis());
        wireOut.getValueOut().text(filter);
        if(status== ReactiveStatus.ERROR){
            wireOut.getValueOut().throwable((Throwable)obj);
        }else {
            wireOut.getValueOut().object(obj);
        }
    }

    public void record(Publisher<?> publisher, String filter) {
        ChronicleQueue queue = reactiveJournal.createQueue();
        ExcerptAppender appender = queue.acquireAppender();

        TriConsumer<ExcerptAppender, String, Object> onNextConsumer = getOnNextConsumerRecorder();
        BiConsumer<ExcerptAppender, String> onCompleteConsumer = getOnCompleteRecorder();
        TriConsumer<ExcerptAppender, String, Throwable> onErrorConsumer = getOnErrorRecorder();

        publisher.subscribe(new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object o) {
                onNextConsumer.accept(appender, filter, o);
            }

            @Override
            public void onError(Throwable throwable) {
                onErrorConsumer.accept(appender, filter, throwable);
            }

            @Override
            public void onComplete() {
                onCompleteConsumer.accept(appender, filter);
            }
        });
    }
}
