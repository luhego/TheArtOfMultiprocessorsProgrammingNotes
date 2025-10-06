package org.monitors;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class GenericLockTester {

    @FunctionalInterface
    public interface Locker {
        void run() throws InterruptedException;
    }

    public static void test(Locker readLock, Locker readUnlock, Locker writeLock, Locker writeUnLock) {
        StringBuilder sharedData = new StringBuilder("Initial");

        Runnable readerTask = () -> {
            String name = Thread.currentThread().getName();
            while (true) {
                try {
                    readLock.run();
                    System.out.println(name + " READING " + sharedData + " at " + System.currentTimeMillis() % 100000);
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    try {
                        readUnlock.run();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        };

        Runnable writerTask = () -> {
            String name = Thread.currentThread().getName();
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }

                try {
                    writeLock.run();
                    sharedData.append(" +W");
                    System.out.println(">>>> " + name + " WRITING... at " + System.currentTimeMillis() % 100000);
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    try {
                        writeUnLock.run();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        // Start several readers
        for (int i = 1; i <= 3; i++) {
            new Thread(readerTask, "Reader-" + i).start();
        }

        // Start writer
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
        new Thread(writerTask, "Writer-1").start();
    }
}