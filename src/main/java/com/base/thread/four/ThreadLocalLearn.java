package com.base.thread.four;

import java.util.Random;

/**
 * @author 十三月之夜
 */
public class ThreadLocalLearn {

    public static void main(String[] args) throws InterruptedException {
        TestThread testThread = new TestThread();
        for (int i = 0; i < 10; ++ i) {
            Thread thread = new Thread(testThread);
            thread.start();
            thread.join();
        }
    }

    private static class TestThread implements Runnable {

        private final ThreadLocal<Integer> integerThreadLocal = new ThreadLocal<>();

        private final ThreadLocal<Random> randomThreadLocal = new ThreadLocal<>();

        @Override
        public void run() {
            randomThreadLocal.set(new Random());
            integerThreadLocal.set(randomThreadLocal.get().nextInt(1000));
            System.out.println(integerThreadLocal.get());
        }
    }
}
