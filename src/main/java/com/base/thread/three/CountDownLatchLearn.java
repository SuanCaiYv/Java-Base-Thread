package com.base.thread.three;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author 十三月之夜
 */
public class CountDownLatchLearn {

    private static final CountDownLatch countDownLatch = new CountDownLatch(10);

    public static void main(String[] args) throws InterruptedException {
        ThreadTest threadTest = new ThreadTest();
        for (int i = 0; i < 10; ++ i) {
            new Thread(threadTest, i + "").start();
        }
        System.out.println("等待子线程完成");
        countDownLatch.await();
        System.out.println("一切就绪! 可以开始工作");
    }

    private static class ThreadTest implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException ignored) {
                ;
            }
            // CountDownLatch 类无法保证线程安全
            System.out.println(Thread.currentThread().getName() + "完成了自己的任务");
            countDownLatch.countDown();
        }
    }
}
