package org.hashset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

public class RefinableHashSet<T> extends BaseHashSet<T> {
    // owner has two values: ref(thread performing the resizing) and mark(whether a resize is happening)
    AtomicMarkableReference<Thread> owner;
    volatile ReentrantLock[] locks;

    public RefinableHashSet(int capacity) {
        super(capacity);
        locks = new ReentrantLock[capacity];
        for (int i = 0; i < capacity; i++) {
            locks[i] = new ReentrantLock();
        }
        owner = new AtomicMarkableReference<Thread>(null, false);
    }

    @Override
    public void acquire(T x) {
        // We use a boolean[] instead of a boolean because AtomicMarkableReference needs a mutable container to write into
        boolean[] mark = {true};
        Thread me = Thread.currentThread(); // current thread calling acquire
        Thread who; // The thread stored in the AtomicMarkableReference(thread currently performing the resize)

        while (true) {

            // Wait while another thread is performing a resize.
            // mark[0] == true  → a resize is in progress
            // who != me        → I am NOT the thread doing the resize
            // Together: keep waiting until resize is finished or until I become the resizer.
            do {
                who = owner.get(mark);
            } while (mark[0] && who != me);

            // Snapshot current locks array
            ReentrantLock[] oldLocks = locks;

            // Lock the stripe based on the snapshot
            ReentrantLock oldLock = oldLocks[x.hashCode() % oldLocks.length];
            oldLock.lock();

            // Re-read resize state
            who = owner.get(mark);
            // Safe to proceed if:
            //  - no resize is happening(mark[0] == false), OR
            //  - I am the resizing thread (who == me)
            // AND
            //  - the locks array has not changed (no resize began mid-lock)
            if ((!mark[0] || who == me) && locks == oldLocks) {
                return;
            } else {
                // Resize interfered → release and retry
                oldLock.unlock();
            }
        }
    }

    @Override
    public void release(T x) {
        locks[x.hashCode() % locks.length].unlock();
    }

    @Override
    public void resize() {
        boolean[] mark = {false};
        Thread me = Thread.currentThread();
        // Attempt to become the resizing thread:
        //   - expectedRef   = null   (no one is resizing yet)
        //   - newRef        = me     (I will be the resizer)
        //   - expectedMark  = false  (no resize in progress)
        //   - newMark       = true   (set resize-in-progress)
        // Succeeds only if no other thread has started resizing.
        if (owner.compareAndSet(null, me, false, true)) {
            try {
                // If another thread already resized and the load is now low, skip resizing.
                if (!policy()) {
                    return;
                }

                // Wait until no thread is holding any of the old locks.
                quiesce();

                int newCapacity = 2 * table.length;
                List<T>[] oldTable = table;
                table = (List<T>[]) new List[newCapacity];
                for (int i = 0; i < newCapacity; i++) {
                    table[i] = new ArrayList<>();
                }
                locks = new ReentrantLock[newCapacity];
                for (int j = 0; j < locks.length; j++) {
                    locks[j] = new ReentrantLock();
                }
                initializeFrom(oldTable);
            } finally {
                // Resize is completed. Disabling resize marker and ref.
                owner.set(null, false);
            }
        }

    }

    protected  void quiesce() {
        for (ReentrantLock lock : locks) {
            while (lock.isLocked()) {}
        }
    }

    protected void initializeFrom(List<T>[] oldTable) {
        // Rehash all elements from oldTable into the newly resized table
        for (List<T> bucket : oldTable) {
            for (T x : bucket) {
                int newIndex = Math.floorMod(x.hashCode(), table.length);
                table[newIndex].add(x);
            }
        }
    }

    @Override
    public boolean policy() {
        return (setSize.get() / table.length) > 4;
    }

}
