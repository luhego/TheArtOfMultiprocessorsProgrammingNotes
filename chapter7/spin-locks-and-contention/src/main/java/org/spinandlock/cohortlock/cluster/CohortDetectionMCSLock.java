package org.spinandlock.cohortlock.cluster;

import org.spinandlock.MCSLock;

public class CohortDetectionMCSLock extends MCSLock implements CohortDetectionLock {
    @Override
    public boolean alone() {
        return myNode.get().getNext() == null;
    }
}
