package org.spinandlock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TASLock implements Lock {
    AtomicBoolean state = new AtomicBoolean(false);

    public void lock() {
        while (state.getAndSet(true)) {
        }
    }

    public void unlock() {
        state.set(false);
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public void lockInterruptibly() {
    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        final int NUM_THREADS = 8;
        final int INCREMENTS_PER_THREAD = 200_000;

        // ---- Test 1: With TasLock ----
        TASLock lock = new TASLock();
        final int[] lockedCounter = {0};

        Runnable lockedTask = () -> {
            for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                lock.lock();
                try {
                    lockedCounter[0]++;
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(lockedTask);
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        System.out.println("With TasLock, final counter = " + lockedCounter[0] +
                " (expected " + (NUM_THREADS * INCREMENTS_PER_THREAD) + ")");

        // ---- Test 2: Without Lock ----
        final int[] plainCounter = {0};
        Runnable plainTask = () -> {
            for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                plainCounter[0]++; // no synchronization
            }
        };

        threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(plainTask);
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        System.out.println("Without lock, final counter = " + plainCounter[0] +
                " (expected " + (NUM_THREADS * INCREMENTS_PER_THREAD) + ")");
    }
}
