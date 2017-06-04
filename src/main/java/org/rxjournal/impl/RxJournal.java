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
        writeToFile(fileOutput, false, null);
    }
    public void writeToFile(String fileOutput, boolean toStdout){
        writeToFile(fileOutput, toStdout, null);
    }

    /**
     * Writes the journal in a human readable form to a file. Optionally also writes it to stdout.
     * @param fileOutput The name of the file
     * @param toStdout Whether it should be written to stdout
     * @param zoneId TimeZone to display to format the time. If null uses millis since 1970.
     */
    public void writeToFile(String fileOutput, boolean toStdout, ZoneId zoneId) {
        LOG.info("Writing recording to dir [" + fileOutput + "]");
        try (ChronicleQueue queue = createQueue()) {
            ExcerptTailer tailer = queue.createTailer();
            try {
                writeQueueToFile(tailer, fileOutput, toStdout, zoneId);
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

    private static void writeQueueToFile(ExcerptTailer tailer, String fileName, boolean toStdout, ZoneId zoneId)
            throws IOException {
        FileWriter fileWriter = new FileWriter(fileName);
        tailer.toStart();
        DataItemProcessor dim = new DataItemProcessor();
        while(tailer.readDocument(
                w -> {
                    ValueIn in = w.getValueIn();
                    dim.process(in,null);
                    String time = null;
                    if(zoneId != null) {
                        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dim.getTime()), zoneId);
                        time = dateTime.toString();
                    }else{
                        time = String.valueOf(dim.getTime());
                    }
                    try {
                        String item = RxStatus.toString(dim.getStatus()) + "\t" + dim.getMessageCount() + "\t" + time + "\t"
                                + dim.getFilter() + "\t" + dim.getObject();
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
