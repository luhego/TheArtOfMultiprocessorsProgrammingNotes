package org.monitors.readerswriters;

import org.monitors.GenericLockTester;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

public class FifoReadWriteLock implements ReadWriteLock {
    int readAcquires, readReleases;
    boolean writer;
    Lock lock;
    Condition condition;
    Lock readLock;
    Lock writeLock;

    public FifoReadWriteLock() {
        readAcquires = 0;
        readReleases = 0;
        writer = false;
        lock = new ReentrantLock();
        condition = lock.newCondition();
        readLock = new ReadLock();
        writeLock = new WriteLock();
    }

    public Lock readLock() {
        return readLock;
    }

    public Lock writeLock() {
        return writeLock;
    }

    private class ReadLock implements Lock {
        public void lock() {
            lock.lock();
            try {
                while (writer)
                    condition.await();
                readAcquires++;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }

        public void unlock() {
            lock.lock();

            try {
                readReleases++;
                if (readAcquires == readReleases)
                    condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }

    private class WriteLock implements Lock {
        public void lock() {
            lock.lock();
            try {
                while (writer)
                    condition.await();
                writer = true;
                while (readAcquires != readReleases)
                    condition.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }

        public void unlock() {
            lock.lock();
            try {
                writer = false;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }

    public static void main(String[] args) {
        FifoReadWriteLock rwLock = new FifoReadWriteLock();
        GenericLockTester.test(
                rwLock.readLock::lock,
                rwLock.readLock::unlock,
                rwLock.writeLock::lock,
                rwLock.writeLock::unlock
        );
    }
}
