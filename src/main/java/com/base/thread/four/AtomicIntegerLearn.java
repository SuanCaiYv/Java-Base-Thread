package com.base.thread.four;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 十三月之夜
 */
public class AtomicIntegerLearn {

    public static void main(String[] args) throws InterruptedException {
        AtomicInteger atomicInteger1 = new AtomicInteger();
        AtomicInteger atomicInteger2 = new AtomicInteger();
        Runnable runnable10 = () -> {
            for (int i = 0; i < 10000; ++ i) {
                int a = atomicInteger1.get();
                atomicInteger1.set(a + 1);
            }
        };
        Runnable runnable20 = () -> {
            for (int i = 0; i < 10000; ++ i) {
                // atomicInteger2.incrementAndGet();
                atomicInteger2.addAndGet(1);
            }
        };
        Thread thread1 = new Thread(runnable10);
        Thread thread2 = new Thread(runnable10);
        Thread thread3 = new Thread(runnable20);
        Thread thread4 = new Thread(runnable20);
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        System.out.println(atomicInteger1.get());
        System.out.println(atomicInteger2.get());
    }
}
