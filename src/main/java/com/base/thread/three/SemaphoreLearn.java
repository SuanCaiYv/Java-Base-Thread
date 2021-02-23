package com.base.thread.three;

import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * @author 十三月之夜
 */
public class SemaphoreLearn {

    private static final Semaphore semaphore = new Semaphore(20);

    public static void main(String[] args) throws InterruptedException {
        MyThread myThread = new MyThread();
        for (int i = 0; i < 10; ++ i) {
            Thread thread = new Thread(myThread, i + "");
            thread.start();
        }
        Thread.sleep(100);
        System.out.println("Done");
    }

    private static class MyThread implements Runnable {

        @Override
        public void run() {
            int a = new Random().nextInt(8);
            System.out.println(Thread.currentThread().getName() + "需要共计: " + a + "个许可才能工作");
            for (int j = 0; j < a; ++ j) {
                while (true) {
                    if (semaphore.tryAcquire()) {
                        System.out.println(Thread.currentThread().getName() + "申请到第: " + (j + 1) + "个许可");
                        break;
                    }
                }
            }
            System.out.println(Thread.currentThread().getName() + "开始工作...");
            for (int j = 0; j < a; ++ j) {
                semaphore.release();
                System.out.println(Thread.currentThread().getName() + "释放第: " + (j + 1) + "个许可");
            }
        }
    }
}
