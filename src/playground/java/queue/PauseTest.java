package queue;

import java.util.concurrent.locks.LockSupport;

/**
 * Created by daniel on 25/05/17.
 */
public class PauseTest {
    public static void main(String[] args) throws InterruptedException {
        long startTime = System.nanoTime();
        int spins = 10_000;
        for (int i = 0; i < spins; i++) {
            Thread.yield();
            //LockSupport.parkNanos(1);
            //Thread.sleep(1);
        }
        System.out.println((System.nanoTime()-startTime)/spins);
    }
}
