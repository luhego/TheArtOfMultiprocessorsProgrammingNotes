package org.spinandlock.cohortlock.global;

import org.spinandlock.BackoffLock;
import org.spinandlock.cohortlock.cluster.ClusterLocal;
import org.spinandlock.cohortlock.cluster.CohortDetectionMCSLock;

public class CohortBackoffMCSLock extends CohortLock {
    public CohortBackoffMCSLock(int passLimit) {
        super(new BackoffLock(), new ClusterLocal<>(CohortDetectionMCSLock::new), passLimit);
    }
}
