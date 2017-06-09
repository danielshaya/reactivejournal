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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RxJournal
 */
public class RxJournal {
    private static final Logger LOG = LoggerFactory.getLogger(RxJournal.class.getName());

    /**
     * The default name for the data directory.
     */
    public static final String RXJOURNAL_DIRNAME = ".rxJournal";

    private String dir;
    private final AtomicLong messageCounter = new AtomicLong(0);

    /**
     * Create an {@link RxJournal} that will store or read its data from the {@value RXJOURNAL_DIRNAME} directory
     * inside the given {@literal baseDir}. Use {@link #getDir()} to obtain the full path of said directory, and
     * {@link #clearCache()} to clean that directory of RxJournal-specific files.
     *
     * @param baseDir the base directory into which the RxJournal will create a RxJournal-dedicated data directory {@value RXJOURNAL_DIRNAME}.
     * @see #getDir()
     * @see #clearCache()
     */
    public RxJournal(String baseDir){
        this.dir = Paths.get(baseDir, RXJOURNAL_DIRNAME).toString();
    }

    /**
     * Create an {@link RxJournal} that will store or read its data from the {@literal journalName} directory
     * inside the given {@literal baseDir}. Use {@link #getDir()} to obtain the full path of said directory, and
     * {@link #clearCache()} to clean that directory of RxJournal-specific files.
     *
     * @param baseDir the base directory into which the RxJournal will create a RxJournal-dedicated data directory.
     * @param journalName the name to use for this RxJournal's data directory, instead of the default {@value RXJOURNAL_DIRNAME}.
     * @see #getDir()
     * @see #clearCache()
     */
    public RxJournal(String baseDir, String journalName){
        this.dir = Paths.get(baseDir, journalName).toString();
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

    /**
     * Return the path to the RxJournal-specific directory used to store binary representation of the journal on disk.
     * This directory is one level deeper than the base directory initially passed to RxJournal's
     * {@link RxJournal#RxJournal(String) constructor}.
     *
     * @return the path to the RxJournal specific directory.
     */
    public String getDir() {
        return dir;
    }

    /**
     * Clear this RxJournal's {@link #getDir() data directory} from RxJournal-specific binary files, and remove it as
     * well (but only if it has become empty). If other files have been created inside the data directory, it is the
     * responsibility of the user to delete them AND the data directory.
     *
     * @throws IOException in case of directory traversal or file deletion problems
     */
    public void clearCache() throws IOException {
        LOG.info("Deleting existing recording [{}]", dir);
        Path journalDir = Paths.get(dir);
        if(Files.exists(journalDir)) {
            Files.walk(journalDir)
                    .map(Path::toFile)
                    .filter(f -> f.isFile() && f.getName().endsWith(".cq4"))
                    .peek(f -> LOG.info("Removing {}", f.getName()))
                    .forEach(File::delete);

            try {
                Files.deleteIfExists(journalDir);
            } catch (DirectoryNotEmptyException e) {
                LOG.info("Directory does not only contain cq4 files, not deleted");
            }
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

    AtomicLong getMessageCounter() {
        return messageCounter;
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
