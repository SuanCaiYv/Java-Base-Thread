package com.base.thread;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntConsumer;

/**
 * @author 十三月之夜
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger();
        Runnable runnable1 = () -> {
            for (int i = 0; i < 1000; ++ i) {
                int a = atomicInteger.get();
                ++ a;
                atomicInteger.set(a);
            }
        };
        Runnable runnable2 = () -> {
            for (int i = 0; i < 1000; ++ i) {
                int a = atomicInteger.get();
                ++ a;
                atomicInteger.set(a);
            }
        };
        Thread thread1 = new Thread(runnable1);
        Thread thread2 = new Thread(runnable2);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        System.out.println(atomicInteger.get());
    }
}
