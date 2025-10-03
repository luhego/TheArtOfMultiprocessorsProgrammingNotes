package org.spinandlock.cohortlock.cluster;

import java.util.function.Supplier;

/**
 * ClusterLocal<T> simulates NUMA-style clusters by grouping threads into
 * a fixed number of clusters. Each cluster shares a single object instance,
 * while threads in different clusters get separate instances.
 * <p>
 * Example:
 * ClusterLocal<Lock> clusterLock = new ClusterLocal<>(MCSLock::new, 2);
 * - Threads are assigned clusterId = threadId % numClusters.
 * - All threads in cluster 0 share one MCSLock.
 * - All threads in cluster 1 share another MCSLock.
 * <p>
 * This allows testing of hierarchical/cohort locks in Java without access
 * to real hardware NUMA topology. It’s useful for simulating local vs global
 * contention: threads in the same cluster contend on the same local lock,
 * while threads in different clusters must escalate to the global lock.
 */
public class ClusterLocal<T> {
    private final Object[] clusterLocks;
    private final int numClusters;

    public ClusterLocal(Supplier<T> supplier) {
        this.numClusters = 4;
        clusterLocks = new Object[numClusters];
        for (int i = 0; i < numClusters; i++) {
            clusterLocks[i] = supplier.get();
        }
    }

    @SuppressWarnings("unchecked")
    public T get() {
        int id = (int) (Thread.currentThread().getId() % numClusters);
        return (T) clusterLocks[id];
    }
}