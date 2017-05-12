package org.rxrecorder.impl;

import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.WireOut;
import org.rxrecorder.util.DSUtil;
import org.rxrecorder.util.QueueUtils;
import org.rxrecorder.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Class to record input from Observables and playback and validate recordings.
 */
public class RxRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(RxRecorder.class.getName());
    private String fileName;
    private String END_OF_STREAM_FILTER = "endOfStream";
    private String ERROR_FILTER = "error";
    private final AtomicLong messageCounter = new AtomicLong(0);

    public enum Replay {REAL_TIME, FAST}

    public Observable play(PlayOptions options) {
        long fromTime = System.currentTimeMillis();

        return Observable.create(subscriber -> {
            try (ChronicleQueue queue = createQueue()) {
                ExcerptTailer tailer = queue.createTailer();
                long[] lastTime = new long[]{Long.MIN_VALUE};
                boolean[] stop = new boolean[]{false};
                while (true) {

                    boolean foundItem = tailer.readDocument(w -> {
                        ValueIn in = w.getValueIn();
                        long messageCount = in.int64();
                        long recordedAtTime = in.int64();
                        String storedWithFilter = in.text();

                        if (testEndOfStream(subscriber, storedWithFilter)) {
                            stop[0] = true;
                            return;
                        }

                        if (testPastPlayUntil(options, subscriber, recordedAtTime)){
                            stop[0] = true;
                            return;
                        }

                        if (options.playFrom() > recordedAtTime
                                && (!options.playFromNow() || fromTime < recordedAtTime)) {
                            pause(options, lastTime, recordedAtTime);

                            if (options.filter().equals(storedWithFilter)) {
                                subscriber.onNext(getStoredObject(options, in));
                            }
                            lastTime[0] = recordedAtTime;
                        }
                    });
                    if (!foundItem && !options.completeAtEndOfFile() || stop[0]) {
                        subscriber.onComplete();
                        return;
                    }
                }
            }

        });
    }

    private boolean testPastPlayUntil(PlayOptions options, Emitter<? super Object> s, long recordedAtTime) {
        if(options.playUntil() > recordedAtTime){
            s.onComplete();
            return true;
        }
        return false;
    }

    private boolean testEndOfStream(Emitter<? super Object> s, String storedWithFilter) {
        if (storedWithFilter.equals(END_OF_STREAM_FILTER)) {
            s.onComplete();
            return true;
        }
        return false;
    }

    private Object getStoredObject(PlayOptions options, ValueIn in) {
        Object storedObject;
        if (options.using() != null) {
            storedObject = in.object(options.using(), options.using().getClass());
        } else {
            storedObject = in.object();
        }
        return storedObject;
    }

    private void pause(PlayOptions options, long[] lastTime, long recordedAtTime) {
        if (options.replayStrategy() == Replay.REAL_TIME && lastTime[0] != Long.MIN_VALUE) {
            DSUtil.sleep((int) (recordedAtTime - lastTime[0]));
        }
        //todo add configurable pause strategy
    }

    public void recordAsync(Observable<?> observable, String filter){
        new Thread(()->record(observable,filter)).start();
    }

    public void recordAsync(Flowable<?> flowable, String filter){
        new Thread(()->record(flowable,filter)).start();
    }

    public void record(Observable<?> observable){
        record(observable, "");
    }

    public void record(Flowable<?> flowable){
        record(flowable, "");
    }

    public void record(Observable<?> observable, String filter) {
        ChronicleQueue queue = createQueue();
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
            writeObject(w, f, v);
        });
    }

    private Consumer<ExcerptAppender> getOnCompleteRecorder(){
        return a -> a.writeDocument(w -> {
            writeObject(w, END_OF_STREAM_FILTER, new EndOfStream());
            LOG.debug("Adding end of stream token");
        });
    }

    private BiConsumer<ExcerptAppender, Throwable> getOnErrorRecorder(){
        return (a, t) -> a.writeDocument(w -> {
            //todo Throwable should go here once Chronicle bug is fixed
            writeObject(w, ERROR_FILTER, t.getMessage());
        });
    }

    private void writeObject(WireOut wireOut, String filter, Object obj){
        wireOut.getValueOut().int64(messageCounter.incrementAndGet());
        wireOut.getValueOut().int64(System.currentTimeMillis());
        wireOut.getValueOut().text(filter);
        wireOut.getValueOut().object(obj);
    }

    public void record(Flowable<?> flowable, String filter) {
        ChronicleQueue queue = createQueue();
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

    public void writeToFile(String fileOutput){
        writeToFile(fileOutput, false);
    }

    public void writeToFile(String fileOutput, boolean toStdout){
        LOG.info("Writing recording to fileName [" + fileOutput + "]");
        try (ChronicleQueue queue = createQueue()) {
            ExcerptTailer tailer = queue.createTailer();
            try {
                QueueUtils.writeQueueToFile(tailer, fileOutput, toStdout);
            } catch (IOException e) {
                LOG.error("Error writing to file", e);
            }
        }
        LOG.info("Writing to fileName complete");
    }

    private ChronicleQueue createQueue(){
        int blockSize = Integer.getInteger("chronicle.queueBlockSize", -1);
        ChronicleQueue queue = null;
        if(blockSize==-1) {
            queue = SingleChronicleQueueBuilder.binary(fileName).build();
        }else {
            queue = SingleChronicleQueueBuilder.binary(fileName).blockSize(blockSize).build();
        }
        return queue;
    }

    public void init(String file, boolean clearCache) throws IOException {
        LOG.info("Initialising RxRecorder on fileName [{}]", file);
        if(clearCache) {
            LOG.info("Deleting existing recording [{}]", file);
            if(Files.exists(Paths.get(file))) {
                Files.walk(Paths.get(file))
                        .map(Path::toFile)
                        .sorted((o1, o2) -> -o1.compareTo(o2))
                        .forEach(File::delete);
                Files.deleteIfExists(Paths.get(file));
            }
        }
        this.fileName = file;
    }
}
