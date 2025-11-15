package org.hashset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int THREADS = 8;
    private static final int ELEMENTS_PER_THREAD = 500_000;
    private static final int INITIAL_CAPACITY = 32;

    public static void main(String[] args) throws InterruptedException {
        List<BaseHashSet<Integer>> sets = List.of(
                new CoarseHashSet<>(INITIAL_CAPACITY),
                new StripedHashSet<>(INITIAL_CAPACITY),
                new RefinableHashSet<>(INITIAL_CAPACITY)
        );

        for (BaseHashSet<Integer> set : sets) {
            System.out.println("---- " + set.getClass().getSimpleName() + " ----");
            var results = benchmark(set);
            printStats("add()", results.get("add"));
            printStats("contains()", results.get("contains"));
            System.out.println();
        }
    }

    private static Map<String, Stats> benchmark(BaseHashSet<Integer> set) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch latch = new CountDownLatch(THREADS);

        List<Long> addTimes = Collections.synchronizedList(new ArrayList<>());
        List<Long> containsTimes = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < THREADS; t++) {
            final int offset = t * ELEMENTS_PER_THREAD;
            pool.execute(() -> {
                for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
                    int val = offset + i;
                    long start = System.nanoTime();
                    set.add(val);
                    long mid = System.nanoTime();
                    set.contains(val);
                    long end = System.nanoTime();
                    addTimes.add(mid - start);
                    containsTimes.add(end - mid);
                }
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();

        return Map.of(
                "add", summarize(addTimes),
                "contains", summarize(containsTimes)
        );
    }

    private static Stats summarize(List<Long> times) {
        if (times.isEmpty()) return new Stats(0, 0, 0, 0);
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        double min = times.stream().mapToLong(Long::longValue).min().orElse(0);
        double max = times.stream().mapToLong(Long::longValue).max().orElse(0);
        return new Stats(avg / 1_000.0, min / 1_000.0, max / 1_000.0, times.size()); // μs
    }

    private static void printStats(String op, Stats s) {
        System.out.printf("%-10s avg=%8.3f μs | min=%6.3f μs | max=%8.3f μs | n=%d%n",
                op, s.avg, s.min, s.max, s.totalOps);
    }

    record Stats(double avg, double min, double max, long totalOps) {
    }
}
