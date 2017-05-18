package org.rxjournal.impl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.ValueIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * RxJournal
 */
public class RxJournal {
    private static final Logger LOG = LoggerFactory.getLogger(RxJournal.class.getName());
    static final String END_OF_STREAM_FILTER = "endOfStream";
    static final String ERROR_FILTER = "error";
    private String dir;

    public RxJournal(String dir){
        this.dir = dir;
    }

    public RxRecorder createRxRecorder(){
        return new RxRecorder(this);
    }

    public RxPlayer createRxPlayer(){
        return new RxPlayer(this);
    }

    public RxValidator createRxValidator(){
        return new RxValidator();
    }

    public void clearCache() throws IOException {
        LOG.info("Deleting existing recording [{}]", dir);
        if(Files.exists(Paths.get(dir))) {
            Files.walk(Paths.get(dir))
                    .map(Path::toFile)
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .forEach(File::delete);
            Files.deleteIfExists(Paths.get(dir));
        }
    }

    public void writeToFile(String fileOutput){
        writeToFile(fileOutput, false);
    }

    public void writeToFile(String fileOutput, boolean toStdout){
        LOG.info("Writing recording to dir [" + fileOutput + "]");
        try (ChronicleQueue queue = createQueue()) {
            ExcerptTailer tailer = queue.createTailer();
            try {
                writeQueueToFile(tailer, fileOutput, toStdout);
            } catch (IOException e) {
                LOG.error("Error writing to file", e);
            }
        }
        LOG.info("Writing to dir complete");
    }

    //todo should this cache the queue - probably
    ChronicleQueue createQueue(){
        int blockSize = Integer.getInteger("chronicle.queueBlockSize", -1);
        ChronicleQueue queue = null;
        if(blockSize==-1) {
            queue = SingleChronicleQueueBuilder.binary(dir).build();
        }else {
            queue = SingleChronicleQueueBuilder.binary(dir).blockSize(blockSize).build();
        }
        return queue;
    }

    private static void writeQueueToFile(ExcerptTailer tailer, String fileName) throws IOException {
        writeQueueToFile(tailer, fileName, false);
    }

    private static void writeQueueToFile(ExcerptTailer tailer, String fileName, boolean toStdout) throws IOException {
        FileWriter fileWriter = new FileWriter(fileName);
        tailer.toStart();
        while(tailer.readDocument(
                w -> {
                    ValueIn in = w.getValueIn();
                    byte status = in.int8();
                    long messageCount = in.int64();
                    long time = in.int64();
                    //todo timezone should be configurable
                    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.of("Europe/London"));
                    String filter = in.text();
                    Object valueFromQueue = in.object();
                    try {
                        String item = RxStatus.toString(status) + "\t" + messageCount + "\t" + dateTime + "\t"
                                + filter + "\t" + valueFromQueue;
                        fileWriter.write(item  + "\n");
                        if(toStdout) {
                            LOG.info(item);
                        }
                    } catch (IOException e) {
                        LOG.error("Problem writing to file[" + fileName + "]", e);
                    }
                }));
        fileWriter.close();
    }
}
