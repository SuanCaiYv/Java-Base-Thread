package com.base.thread.three;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 十三月之夜
 */
public class SemaphoreTest {

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        Semaphore semaphore = new Semaphore(0);
        Runnable runnable1 = () -> {
            try {
                semaphore.acquire(2);
            } catch (InterruptedException ignored) {
                ;
            }
            semaphore.release(2);
            System.out.println(Thread.currentThread().getName() + ": R1Get");
        };
        Runnable runnable2= () -> {
            try {
                semaphore.acquire(1);
            } catch (InterruptedException ignored) {
                ;
            }
            semaphore.release();
            System.out.println(Thread.currentThread().getName() + ": R2Get");
        };
        new Thread(runnable1).start();
        LockSupport.parkNanos(50 * 1000 * 1000);
        new Thread(runnable2).start();
        LockSupport.parkNanos(50 * 1000 * 1000);
        new Thread(runnable2).start();
        LockSupport.parkNanos(50 * 1000 * 1000);
        new Thread(runnable2).start();
        LockSupport.parkNanos(50 * 1000 * 1000);
        semaphore.release();
    }
}

