package org.reactivejournal.util;

/**
 * Created by daniel on 27/04/17.
 */
public class DSUtil {
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void exitAfter(int timeMs) {
        new Thread(()->{
            sleep(timeMs);
            System.out.println("Exiting after waiting " + timeMs + "ms");
            System.exit(0);
        }).start();
    }
}
