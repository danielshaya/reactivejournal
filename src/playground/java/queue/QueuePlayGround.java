package queue;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.ValueIn;
import org.rxrecorder.examples.fastproducerslowconsumer.MarketData;
import org.rxrecorder.util.DSUtil;

import java.util.concurrent.Executors;

/**
 * Created by daniel on 19/10/16.
 */
public class QueuePlayGround {
    private static String file = "/tmp/playground";

    public static void main(String[] args) {
        writeThrowableToQueue();
    }

    public static void writeThrowableToQueue(){
        final Throwable throwable;
        try {
            throw new RuntimeException("serialise");
        }catch(Throwable e){
            throwable = e;
        }

        try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).build()) {
            final ExcerptAppender appender = queue.acquireAppender();
                appender.writeDocument(w -> {
                    w.getValueOut().int64(System.currentTimeMillis());
                    w.getValueOut().object(throwable);
                });
        }

        try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).build()) {
            final ExcerptTailer tailer = queue.createTailer();
            tailer.readDocument(w -> {
                ValueIn in = w.getValueIn();
                long time = in.int64();
                System.out.println(in.object());
            });
        }

    }

    public static void writeNonSerial() {
        //for(int t=0; t<5; t++) {
        //    new Thread(() -> {
        try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).bufferCapacity(1000).build()) {
            final ExcerptAppender appender = queue.acquireAppender();
            for (int i = 0; i < 10; i++) {
                final int f = i;
                appender.writeDocument(w -> {
                    w.getValueOut().int64(System.currentTimeMillis());
                    w.getValueOut().object(new MarketData());
                });
                DSUtil.sleep(100);
            }

        }
        //    }).start();
        // }
    }

    public static void writeToQueue() {
        //for(int t=0; t<5; t++) {
        //    new Thread(() -> {
        try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).build()) {
            final ExcerptAppender appender = queue.acquireAppender();
            for (int i = 0; i < 10; i++) {
                final int f = i;
                appender.writeDocument(w -> {
                    w.getValueOut().int64(System.currentTimeMillis());
                    w.getValueOut().object(new MarketData());
                });
                DSUtil.sleep(100);
            }

        }
        //    }).start();
        // }
    }

    public static void readFromQueue(){

        ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).build();

            final ExcerptTailer tailer = queue.createTailer();
            //System.out.println(queue.dump());
            Executors.newSingleThreadExecutor().submit(()-> {
                while (true) {
                    tailer.readDocument(w -> {
                        ValueIn in = w.getValueIn();
                        long time = in.int64();
                        String filter = in.text();
                        MarketData trade = in.object(new MarketData(), MarketData.class);
                        System.out.println(time + "->" + trade);
                    });
                }
            });

    }

    public static void dumpQueue(){

        try(ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).build()){
            System.out.println(queue.dump());
        }
    }
}
