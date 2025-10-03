package org.spinandlock.cohortlock.cluster;

import java.util.concurrent.locks.Lock;

public interface CohortDetectionLock extends Lock {
    public boolean alone();
}
