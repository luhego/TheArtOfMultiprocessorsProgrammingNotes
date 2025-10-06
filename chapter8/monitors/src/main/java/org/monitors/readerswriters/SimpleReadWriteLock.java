package org.monitors.readerswriters;

import org.monitors.GenericLockTester;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleReadWriteLock implements ReadWriteLock {
    int readers;
    boolean writer;

    Lock lock;
    Condition condition;
    Lock readLock;
    Lock writeLock;

    public SimpleReadWriteLock() {
        writer = false;
        readers = 0;
        lock = new ReentrantLock();
        readLock = new ReadLock();
        writeLock = new WriteLock();
        condition = lock.newCondition();
    }

    public Lock readLock() {
        return readLock;
    }

    public Lock writeLock() {
        return writeLock;
    }

    class ReadLock implements Lock {
        public void lock() {
            lock.lock();
            try {
                while (writer) {
                    condition.await();
                }
                readers++;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }

        public void unlock() {
            lock.lock();
            try {
                readers--;
                if (readers == 0)
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

    protected class WriteLock implements Lock {
        public void lock() {
            lock.lock();
            try {
                while (readers > 0 || writer)
                    condition.await();
                writer = true;
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
        SimpleReadWriteLock rwLock = new SimpleReadWriteLock();
        GenericLockTester.test(
                rwLock.readLock::lock,
                rwLock.readLock::unlock,
                rwLock.writeLock::lock,
                rwLock.writeLock::unlock
        );
    }
}
