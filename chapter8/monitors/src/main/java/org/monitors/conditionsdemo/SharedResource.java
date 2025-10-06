package org.monitors.conditionsdemo;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SharedResource {
    private int data = 0;
    private boolean available = false;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public void produce() throws InterruptedException {
        lock.lock();

        try {
            while (available) { // wait if previous data not consumed
                condition.await();
            }
            data++;
            System.out.println("Produced: " + data);
            available = true;
            condition.signalAll(); // wake up waiting consumers
        } finally {
            lock.unlock();
        }
    }

    public void consume() throws InterruptedException {
        lock.lock();

        try {
            while (!available) {    // wait until data is available
                condition.await();
            }
            System.out.println("Consumed " + data);
            available = false;      // mark resource as empty
            condition.signalAll();  // wake up waiting producers
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    resource.produce();
//                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    resource.consume();
//                    Thread.sleep(400);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
    }

}

