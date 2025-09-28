package org.spinandlock;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public class Backoff {
    final int minDelay;
    final int maxDelay;
    int limit;

    public Backoff(int min, int max) {
        minDelay = min;
        maxDelay = max;
        limit = minDelay;
    }

//    public void backoff() throws InterruptedException {
//        int delay = ThreadLocalRandom.current().nextInt(limit);
//        limit = Math.min(maxDelay, 2 * limit);
//        Thread.sleep(delay);
//    }

    public void backoff() {
        int delay = ThreadLocalRandom.current().nextInt(limit);
        limit = Math.min(maxDelay, 2 * limit);
        LockSupport.parkNanos(delay);
    }
}
