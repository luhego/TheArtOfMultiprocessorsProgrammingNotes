package org.monitors.exercises;

import org.monitors.GenericLockTester;
import org.monitors.readerswriters.FifoReadWriteLock;

/**
 * Exercise 8.1 Implement SimpleReadWriteLock using synchronized instead of locks and conditions
 */
public class SimpleReadWriteLockSync {
    private int readers;
    private boolean writer;
    private final ReadLock readLock;
    private final WriteLock writeLock;

    public SimpleReadWriteLockSync() {
        readers = 0;
        writer = false;
        readLock = new ReadLock();
        writeLock = new WriteLock();
    }

    public ReadLock readLock() {
        return readLock;
    }

    public WriteLock writeLock() {
        return writeLock;
    }

    class ReadLock {
        public synchronized void lock() throws InterruptedException {
            while (writer) {
                wait();
            }
            readers++;
        }

        public void unlock() {
            readers--;
            if (readers == 0)
                notifyAll();
        }

    }

    protected class WriteLock {
        public synchronized void lock() throws InterruptedException {
            while (readers > 0 || writer)
                wait();
            writer = true;
        }

        public synchronized void unlock() {
            writer = false;
            notifyAll();
        }
    }

    public static void main(String[] args) {
        SimpleReadWriteLockSync rwLock = new SimpleReadWriteLockSync();
        GenericLockTester.test(
                rwLock.readLock::lock,
                rwLock.readLock::unlock,
                rwLock.writeLock::lock,
                rwLock.writeLock::unlock
        );
    }
}
