package org.spinandlock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class BackoffLock implements Lock {
    AtomicBoolean state = new AtomicBoolean(false);
    private static final int MIN_DELAY_NS = 100;
    private static final int MAX_DELAY_NS = 10_000;

    public void lock() {
        Backoff backoff = new Backoff(MIN_DELAY_NS, MAX_DELAY_NS);
        while (true) {
            while (state.get()) {
            }
            if (!state.getAndSet(true)) {
                return;
            } else {
                backoff.backoff();
            }
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
}
