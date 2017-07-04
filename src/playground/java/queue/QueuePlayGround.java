package queue;

import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.ValueIn;
import org.reactivejournal.util.DSUtil;
import sun.misc.Unsafe;

import java.nio.charset.StandardCharsets;
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

            appender.writeBytes(b -> {
                long address = b.address(b.writePosition());
                Unsafe unsafe = UnsafeMemory.UNSAFE;
                unsafe.putByte(address, (byte) 0x12);
                address += 1;
                unsafe.putInt(address, 0x345678);
                address += 4;
                unsafe.putLong(address, 0x999000999000L);
                address += 8;
                byte[] bytes = "Hello World".getBytes(StandardCharsets.ISO_8859_1);
                unsafe.putByte(address, (byte)bytes.length);
                address += 1;
                unsafe.copyMemory(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, address, bytes.length);
                b.writeSkip(1 + 4 + 8 + bytes.length);
            });


            final ExcerptTailer tailer = queue.createTailer();
            tailer.readBytes(b -> {
                long address = b.address(b.readPosition());
                Unsafe unsafe = UnsafeMemory.UNSAFE;
                int code = unsafe.getByte(address);
                address++;
                int num = unsafe.getInt(address);
                address += 4;
                long num2 = unsafe.getLong(address);
                address += 8;
                int length = unsafe.getByte(address);
                address++;
                byte[] bytes = new byte[length];
                unsafe.copyMemory(null, address, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, bytes.length);
                String text = new String(bytes, StandardCharsets.ISO_8859_1);
                System.out.println(text);
                // do something with values
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
