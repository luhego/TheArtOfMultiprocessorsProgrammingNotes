package org.spinandlock.cohortlock;

import org.spinandlock.cohortlock.global.CohortBackoffMCSLock;
import org.spinandlock.cohortlock.global.CohortLock;

import java.util.concurrent.locks.Lock;

public class CohortLockTestHarness {
    static void runTest(Lock lock, String name, int threads, int iters) throws InterruptedException {
        final int[] counter = {0};
        long start = System.nanoTime();

        Runnable task = () -> {
            for (int i = 0; i < iters; i++) {
                lock.lock();
                try {
                    counter[0]++;
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread[] arr = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            arr[i] = new Thread(task, "T-" + i);
            arr[i].start();
        }
        for (Thread t : arr) t.join();
        long end = System.nanoTime();

        System.out.println(name + " => counter=" + counter[0] +
                " (expected " + (threads * iters) + ")" +
                ", time=" + (end - start) / 1_000_000 + " ms");

        if (lock instanceof CohortLock) {
            CohortLock.printStats(name);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threads = 16;
        int iters = 100_000;

        // Example: BackoffLock for global + MCS for local
        Lock lock = new CohortBackoffMCSLock(5);

        runTest(lock, "CohortBackoffMCSLock", threads, iters);
    }
}
