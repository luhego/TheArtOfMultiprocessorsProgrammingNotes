package org.hashset;


import java.util.List;
import java.util.concurrent.*;

public class MainV2 {
    private static final int THREADS = 8;
    private static final int OPS_PER_THREAD = 500_000;
    private static final int INITIAL_CAPACITY = 32;

    public static void main(String[] args) throws InterruptedException {
        List<BaseHashSet<Integer>> sets = List.of(
                new CoarseHashSet<>(INITIAL_CAPACITY),
                new StripedHashSet<>(INITIAL_CAPACITY),
                new RefinableHashSet<>(INITIAL_CAPACITY)
        );

        for (BaseHashSet<Integer> set : sets) {
            System.out.println("---- Benchmark: " + set.getClass().getSimpleName() + " ----");
            Stats results = runBenchmark(set);
            printResults(results);
            System.out.println();
        }
    }

    static class ThreadStats {
        long addNanos = 0;
        long containsNanos = 0;
        long removeNanos = 0;
        long ops = 0;
    }

    static class Stats {
        long totalAdds;
        long totalContains;
        long totalRemoves;
        long ops;
        long nanos;
    }

    private static Stats runBenchmark(BaseHashSet<Integer> set) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        CountDownLatch latch = new CountDownLatch(THREADS);
        List<ThreadStats> results = new CopyOnWriteArrayList<>();

        long startWall = System.nanoTime();

        for (int t = 0; t < THREADS; t++) {
            pool.execute(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                ThreadStats ts = new ThreadStats();

                for (int i = 0; i < OPS_PER_THREAD; i++) {
                    int key = rnd.nextInt(5_000_000);

                    // 40% add, 50% contains, 10% remove
                    int op = rnd.nextInt(100);

                    if (op < 40) {
                        long t1 = System.nanoTime();
                        set.add(key);
                        ts.addNanos += System.nanoTime() - t1;
                    } else if (op < 90) {
                        long t1 = System.nanoTime();
                        set.contains(key);
                        ts.containsNanos += System.nanoTime() - t1;
                    } else {
                        long t1 = System.nanoTime();
                        // you may not have remove() implemented; if not, skip.
                        // set.remove(key);
                        ts.removeNanos += System.nanoTime() - t1;
                    }

                    ts.ops++;
                }

                results.add(ts);
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        long endWall = System.nanoTime();

        return aggregateResults(results, endWall - startWall);
    }

    private static Stats aggregateResults(List<ThreadStats> list, long wallTimeNanos) {
        Stats s = new Stats();

        for (ThreadStats ts : list) {
            s.totalAdds += ts.addNanos;
            s.totalContains += ts.containsNanos;
            s.totalRemoves += ts.removeNanos;
            s.ops += ts.ops;
        }

        s.nanos = wallTimeNanos;
        return s;
    }

    private static void printResults(Stats s) {
        double addAvg = s.totalAdds / (double) s.ops / 1_000.0;
        double containsAvg = s.totalContains / (double) s.ops / 1_000.0;
        double removeAvg = s.totalRemoves / (double) s.ops / 1_000.0;

        System.out.printf("Total ops: %,d\n", s.ops);
        System.out.printf("Wall time: %.2f ms\n\n", s.nanos / 1_000_000.0);

        System.out.printf("add() avg      %8.3f μs\n", addAvg);
        System.out.printf("contains() avg %8.3f μs\n", containsAvg);
//        System.out.printf("remove() avg   %8.3f μs\n", removeAvg);
    }
}
