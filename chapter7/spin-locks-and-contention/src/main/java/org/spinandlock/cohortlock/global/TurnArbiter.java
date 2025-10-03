package org.spinandlock.cohortlock.global;

public class TurnArbiter {
    private final int TURN_LIMIT;
    private int turns = 0;

    public TurnArbiter(int limit) {
        TURN_LIMIT = limit;
    }

    public boolean goAgain() {
        return turns < TURN_LIMIT;
    }

    public void wentAgain() {
        turns++;
    }

    public void passed() {
        turns = 0;
    }
}
