package com.base.thread.three;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @author 十三月之夜
 */
public class CyclicBarrierLearn {

    private static final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("此次循环完成");
        }
    };

    private static final CyclicBarrier cyclicBarrier = new CyclicBarrier(5, runnable);

    public static void main(String[] args) throws InterruptedException {
        ThreadTest threadTest = new ThreadTest();
        for (int i = 0; i < 150; ++ i) {
            new Thread(threadTest, i + "").start();
        }
        Thread.sleep(1000);
        System.out.println(threadTest.counter);
    }

    private static class ThreadTest implements Runnable {

        private int counter = 0;

        @Override
        public void run() {
            // 此类不能保证并发安全，仅仅作为聚合指定数量线程一起工作使用
            for (int i = 0; i < 10000; ++ i) {
                ++ counter;
            }
            System.out.println(Thread.currentThread().getName() + "完成了自己的任务");
            try {
                System.out.println("等待其余线程集合完毕");
                cyclicBarrier.await();
                System.out.println("此次集合完毕");
            } catch (InterruptedException | BrokenBarrierException ignored) {
            }
        }
    }
}
