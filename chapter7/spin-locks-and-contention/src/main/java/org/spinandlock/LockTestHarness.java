package org.spinandlock;

import java.util.concurrent.locks.Lock;

public class LockTestHarness {
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
            arr[i] = new Thread(task);
            arr[i].start();
        }
        for (Thread t : arr) t.join();
        long end = System.nanoTime();

        System.out.println(name + " => counter=" + counter[0] +
                " (expected " + (threads * iters) + ")" +
                ", time=" + (end - start) / 1_000_000 + " ms");
    }

    static void runPlain(int threads, int iters) throws InterruptedException {
        final int[] counter = {0};
        long start = System.nanoTime();

        Runnable task = () -> {
            for (int i = 0; i < iters; i++) {
                counter[0]++;
            }
        };

        Thread[] arr = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            arr[i] = new Thread(task);
            arr[i].start();
        }
        for (Thread t : arr) t.join();
        long end = System.nanoTime();

        System.out.println("NoLock => counter=" + counter[0] +
                " (expected " + (threads * iters) + ")" +
                ", time=" + (end - start) / 1_000_000 + " ms");
    }

    public static void main(String[] args) throws InterruptedException {
        int threads = 8;
        int iters = 2_000_000;

        runTest(new TASLock(), "TasLock", threads, iters);
        runTest(new TTASLock(), "TTASLock", threads, iters);
        runTest(new BackoffLock(), "BackoffLock", threads, iters);
        runTest(new ALock(threads), "ALock", threads, iters);
        runTest(new CLHLock(), "CLHLock", threads, iters);
        runTest(new MCSLock(), "MCSLock", threads, iters);
        runPlain(threads, iters);
    }
}