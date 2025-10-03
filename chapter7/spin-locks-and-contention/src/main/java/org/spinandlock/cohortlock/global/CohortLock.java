package org.spinandlock.cohortlock.global;

import org.spinandlock.cohortlock.cluster.ClusterLocal;
import org.spinandlock.cohortlock.cluster.CohortDetectionLock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

public class CohortLock implements Lock {
    final Lock globalLock;
    final ClusterLocal<CohortDetectionLock> clusterLock;
    final TurnArbiter localPassArbiter;
    final ClusterLocal<AtomicBoolean> passedLocally;

    // Additional variable to get some stats
    static final AtomicInteger localPasses = new AtomicInteger(0);
    static final AtomicInteger globalAcquires = new AtomicInteger(0);
    static final AtomicLong localTime = new AtomicLong(0);
    static final AtomicLong globalTime = new AtomicLong(0);

    public CohortLock(Lock gl, ClusterLocal<CohortDetectionLock> cl, int passLimit) {
        this.globalLock = gl;
        this.clusterLock = cl;
        this.localPassArbiter = new TurnArbiter(passLimit);
        this.passedLocally = new ClusterLocal<>(AtomicBoolean::new);
    }

    @Override
    public void lock() {
        long start = System.nanoTime();

        clusterLock.get().lock();
        if (passedLocally.get().get()) {
            localPasses.incrementAndGet();
            localTime.addAndGet(System.nanoTime() - start);
            return;
        }
        globalLock.lock();

        globalAcquires.incrementAndGet();
        globalTime.addAndGet(System.nanoTime() - start);
    }

    @Override
    public void unlock() {
        CohortDetectionLock cl = clusterLock.get();
        if (cl.alone() || !localPassArbiter.goAgain()) {
            localPassArbiter.passed();
            passedLocally.get().set(false);
            globalLock.unlock();
        } else {
            localPassArbiter.wentAgain();
            passedLocally.get().set(true);
        }
        cl.unlock();
    }

    @Override
    public void lockInterruptibly() {
    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, java.util.concurrent.TimeUnit unit) {
        return false;
    }

    @Override
    public java.util.concurrent.locks.Condition newCondition() {
        return null;
    }

    // helper to print stats
    public static void printStats(String name) {
        System.out.printf("%s: localPasses=%d, globalAcquires=%d, avgLocalTime=%d ns, avgGlobalTime=%d ns%n",
                name,
                localPasses.get(),
                globalAcquires.get(),
                (localPasses.get() == 0 ? 0 : localTime.get() / localPasses.get()),
                (globalAcquires.get() == 0 ? 0 : globalTime.get() / globalAcquires.get()));
    }
}
