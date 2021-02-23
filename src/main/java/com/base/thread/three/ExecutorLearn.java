package com.base.thread.three;

import java.util.concurrent.*;

/**
 * @author 十三月之夜
 */
public class ExecutorLearn {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ForkJoinTaskTest forkJoinTaskTest = new ForkJoinTaskTest(0, 1000000);
        long a = System.nanoTime();
        Future<Integer> sum = forkJoinPool.submit(forkJoinTaskTest);
        long b = System.nanoTime();
        System.out.println(b - a);
        System.out.println(sum.get());
    }

    private static class ForkJoinTaskTest extends ForkJoinTask<Integer> {

        private final int a;

        private final int b;

        private int sum = 0;

        public ForkJoinTaskTest(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public Integer getRawResult() {
            return sum;
        }

        @Override
        protected void setRawResult(Integer value) {
            this.sum = value;
        }

        @Override
        protected boolean exec() {
            if (a == b) {
                sum = a;
            } else {
                ForkJoinTaskTest forkJoinTaskTest1 = new ForkJoinTaskTest(a, ((a + b) / 2));
                ForkJoinTaskTest forkJoinTaskTest2 = new ForkJoinTaskTest(((a + b) / 2) + 1, b);
                forkJoinTaskTest1.fork();
                forkJoinTaskTest2.fork();
                sum = forkJoinTaskTest1.join() + forkJoinTaskTest2.join();
            }
            return true;
        }
    }
}
