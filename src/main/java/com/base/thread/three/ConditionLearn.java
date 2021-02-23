package com.base.thread.three;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 十三月之夜
 */
public class ConditionLearn {

    private static final ReentrantLock reentrantLock = new ReentrantLock();

    private static final Condition condition = reentrantLock.newCondition();

    public static void main(String[] args) throws InterruptedException {
        MyThread1 myThread1 = new MyThread1();
        MyThread2 myThread2 = new MyThread2();
        for (int i = 0; i < 20; ++ i) {
            if (i % 2 == 0) {
                Thread thread = new Thread(myThread1, i + "");
                thread.start();
            } else {
                Thread thread = new Thread(myThread2, i + "");
                thread.start();
            }
        }
        Thread.sleep(1000);
    }

    private static class MyThread1 implements Runnable {

        @Override
        public void run() {
            if (reentrantLock.tryLock()) {
                try {
                    System.out.println(Thread.currentThread().getName() + "线程休眠");
                    condition.await();
                    System.out.println(Thread.currentThread().getName() + "被唤醒");
                } catch (InterruptedException ignored) {
                    ;
                } finally {
                    reentrantLock.unlock();
                }
            }
        }
    }

    private static class MyThread2 implements Runnable {

        @Override
        public void run() {
            if (reentrantLock.tryLock()) {
                try {
                    condition.signal();
                    System.out.println(Thread.currentThread().getName() + "线程唤醒");
                } finally {
                    reentrantLock.unlock();
                }
            }
        }
    }
}
