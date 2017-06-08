package org.rxjournal.impl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.WireOut;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.rxjournal.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * Class to record input into RxJournal.
 */
public class RxRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(RxRecorder.class.getName());

    private RxJournal rxJournal;

    public RxRecorder(RxJournal rxJournal) {
        this.rxJournal = rxJournal;
    }

    public void recordAsync(Publisher<?> publisher, String filter){
        new Thread(()->record(publisher,filter)).start();
    }


    private TriConsumer<ExcerptAppender, String, Object> getOnNextConsumerRecorder(){
        return (a, f, v) -> a.writeDocument(w -> {
            writeObject(w, f, v, RxStatus.VALID);
        });
    }

    private BiConsumer<ExcerptAppender, String> getOnCompleteRecorder(){
        return (a,f) -> a.writeDocument(w -> {
            writeObject(w, f, new EndOfStream(), RxStatus.COMPLETE);
            LOG.debug("Adding end of stream token");
        });
    }

    private TriConsumer<ExcerptAppender, String, Throwable> getOnErrorRecorder(){
        return (a, f, t) -> a.writeDocument(w -> {
            writeObject(w, f, t, RxStatus.ERROR);
        });
    }

    private void writeObject(WireOut wireOut, String filter, Object obj, byte status){
        wireOut.getValueOut().int8(status);
        wireOut.getValueOut().int64(rxJournal.getMessageCounter().incrementAndGet());
        wireOut.getValueOut().int64(System.currentTimeMillis());
        wireOut.getValueOut().text(filter);
        if(status==RxStatus.ERROR){
            wireOut.getValueOut().throwable((Throwable)obj);
        }else {
            wireOut.getValueOut().object(obj);
        }
    }

    public void record(Publisher<?> publisher, String filter) {
        ChronicleQueue queue = rxJournal.createQueue();
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
