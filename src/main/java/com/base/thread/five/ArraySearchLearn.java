package com.base.thread.five;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 十三月之夜
 */
public class ArraySearchLearn {

    private static int[] array;

    private static final AtomicInteger atomicInteger = new AtomicInteger(-1);

    private static int finder;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        array = new int[10000];
        Random random = new Random();
        for (int i = 0; i < 10000; ++ i) {
            array[i] = random.nextInt();
        }
        finder = array[6789];
        int slice = 10000 / Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<FutureTask<Integer>> futureTasks = new ArrayList<>();
        int rcd = 0;
        for (int i = 0; i + slice < 10000; i += slice) {
            FutureTask<Integer> futureTask = new FutureTask<>(new Search(i, i + slice - 1));
            executorService.submit(futureTask);
            futureTasks.add(futureTask);
            rcd = i;
        }
        FutureTask<Integer> futureTask = new FutureTask<>(new Search(rcd, 10000 - 1));
        executorService.submit(futureTask);
        futureTasks.add(futureTask);
        for (FutureTask<Integer> f : futureTasks) {
            if (f.get() != -1) {
                System.out.println(f.get());
                break;
            }
        }
        executorService.shutdown();
    }

    private static class Search implements Callable<Integer> {

        private final int from;

        private final int to;

        public Search(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Integer call() throws Exception {
            for (int i = from; i <= to; ++ i) {
                if (atomicInteger.get() != -1) {
                    return atomicInteger.get();
                }
                if (array[i] == finder) {
                    atomicInteger.compareAndSet(-1, i);
                    return i;
                }
            }
            return -1;
        }
    }
}
