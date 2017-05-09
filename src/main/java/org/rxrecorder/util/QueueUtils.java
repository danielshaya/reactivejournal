package org.rxrecorder.util;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.ValueIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Useful queue utilities.
 */
public class QueueUtils {
    private static final Logger LOG = LoggerFactory.getLogger(QueueUtils.class.getName());

    public static void writeQueueToFile(ExcerptTailer tailer, String fileName) throws IOException {
        writeQueueToFile(tailer, fileName, false);
    }

    public static void writeQueueToFile(ExcerptTailer tailer, String fileName, boolean toStdout) throws IOException {
        FileWriter fileWriter = new FileWriter(fileName);
        tailer.toStart();
        while(tailer.readDocument(
                w -> {
                    ValueIn in = w.getValueIn();
                    long time = in.int64();
                    //todo timezone should be configurable
                    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.of("Europe/London"));
                    String filter = in.text();
                    Object valueFromQueue = in.object();
                    try {
                        String item = "" + dateTime + "\t" + filter + "\t" + valueFromQueue;
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
    public static void main(String[] args) throws IOException {
        ChronicleQueue queue = SingleChronicleQueueBuilder.binary("/tmp/Demo").build();
        ExcerptTailer tailer = queue.createTailer();
        writeQueueToFile(tailer, "/tmp/Demo.txt");

//        long index = tailer.index();
//        tailer.index();
//        while (true) {
//            tailer.moveToIndex(index);
//            DocumentContext dc = tailer.readingDocument();
//
//            if(dc.isPresent()==false)return;
//            ValueIn in = dc.wire().getValueIn();
//            long time = in.int64();
//                        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.of("Europe/London"));
//                        String storedFilter = in.text();
//            Object valueFromQueue = in.object();
//            System.out.println("value -> " + valueFromQueue);
//            index++;
//        }

    }
}
