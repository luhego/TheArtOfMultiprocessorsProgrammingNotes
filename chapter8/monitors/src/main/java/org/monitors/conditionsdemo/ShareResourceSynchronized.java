package org.monitors.conditionsdemo;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SharedResourceSynchronized {
    private int data = 0;
    private boolean available = false;

    public synchronized void produce() throws InterruptedException {
        while (available) { // wait if previous data not consumed
            wait();
        }
        data++;
        System.out.println("Produced: " + data);
        available = true;
        notifyAll();
    }

    public synchronized void consume() throws InterruptedException {
        while (!available) {    // wait until data is available
            wait();
        }
        System.out.println("Consumed " + data);
        available = false;      // mark resource as empty
        notifyAll();  // wake up waiting producers
    }

    public static void main(String[] args) {
        SharedResourceSynchronized resource = new SharedResourceSynchronized();

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

