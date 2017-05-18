package org.rxjournal.impl;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.WireOut;
import org.rxjournal.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Class to record input into RxJournal.
 */
public class RxRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(RxRecorder.class.getName());
    private static final AtomicLong messageCounter = new AtomicLong(0);
    private RxJournal rxJournal;

    public RxRecorder(RxJournal rxJournal) {
        this.rxJournal = rxJournal;
    }

    public void recordAsync(Flowable<?> flowable, String filter){
        new Thread(()->record(flowable,filter)).start();
    }

    public void recordAsync(Observable<?> flowable, String filter){
        new Thread(()->record(flowable,filter)).start();
    }

    public void record(Observable<?> observable){
        record(observable, "");
    }

    public void record(Flowable<?> flowable){
        record(flowable, "");
    }

    public void record(Observable<?> observable, String filter) {
        ChronicleQueue queue = rxJournal.createQueue();
        ExcerptAppender appender = queue.acquireAppender();

        TriConsumer<ExcerptAppender, String, Object> onNextConsumer = getOnNextConsumerRecorder();
        Consumer<ExcerptAppender> onCompleteConsumer = getOnCompleteRecorder();
        BiConsumer<ExcerptAppender, Throwable> onErrorConsumer = getOnErrorRecorder();

        observable.subscribe(
                t -> onNextConsumer.accept(appender, filter, t),
                e -> onErrorConsumer.accept(appender, e),
                () -> onCompleteConsumer.accept(appender)
        );
    }

    private TriConsumer<ExcerptAppender, String, Object> getOnNextConsumerRecorder(){
        return (a, f, v) -> a.writeDocument(w -> {
            writeObject(w, f, v, RxStatus.VALID);
        });
    }

    private Consumer<ExcerptAppender> getOnCompleteRecorder(){
        return a -> a.writeDocument(w -> {
            writeObject(w, RxJournal.END_OF_STREAM_FILTER, new EndOfStream(), RxStatus.COMPLETE);
            LOG.debug("Adding end of stream token");
        });
    }

    private BiConsumer<ExcerptAppender, Throwable> getOnErrorRecorder(){
        return (a, t) -> a.writeDocument(w -> {
            //todo Throwable should go here once Chronicle bug is fixed
            writeObject(w, RxJournal.ERROR_FILTER, t.getMessage(), RxStatus.ERROR);
        });
    }

    private void writeObject(WireOut wireOut, String filter, Object obj, byte status){
        wireOut.getValueOut().int8(status);
        wireOut.getValueOut().int64(messageCounter.incrementAndGet());
        wireOut.getValueOut().int64(System.currentTimeMillis());
        wireOut.getValueOut().text(filter);
        wireOut.getValueOut().object(obj);
    }

    public void record(Flowable<?> flowable, String filter) {
        ChronicleQueue queue = rxJournal.createQueue();
        ExcerptAppender appender = queue.acquireAppender();

        TriConsumer<ExcerptAppender, String, Object> onNextConsumer = getOnNextConsumerRecorder();
        Consumer<ExcerptAppender> onCompleteConsumer = getOnCompleteRecorder();
        BiConsumer<ExcerptAppender, Throwable> onErrorConsumer = getOnErrorRecorder();

        flowable.subscribe(
            t -> onNextConsumer.accept(appender, filter, t),
            e -> onErrorConsumer.accept(appender, e),
            () -> onCompleteConsumer.accept(appender)
        );
    }
}
