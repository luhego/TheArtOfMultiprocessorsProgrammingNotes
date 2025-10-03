package org.spinandlock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class MCSLock implements Lock {
    AtomicReference<QNode> tail;
    protected ThreadLocal<QNode> myNode;

    public MCSLock() {
        tail = new AtomicReference<QNode>(null);
        myNode = new ThreadLocal<QNode>() {
            protected QNode initialValue() {
                return new QNode();
            }
        };
    }

    public void lock() {
        QNode qNode = myNode.get();
        QNode pred = tail.getAndSet(qNode);
        if (pred != null) {
            qNode.locked = true;
            pred.next = qNode;
            // wait until predecessor gives up the lock
            while (qNode.locked) {
            }
        }
    }

    public void unlock() {
        QNode qNode = myNode.get();
        if (qNode.next == null) {
            if (tail.compareAndSet(qNode, null))
                return;
            // wait until successor fills in its next field
            while (qNode.next == null) {
            }
        }

        qNode.next.locked = false;
        qNode.next = null;
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

    protected class QNode {
        volatile boolean locked = false;
        volatile QNode next = null;

        public QNode getNext() {
            return next;
        }
    }
}
