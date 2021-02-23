package com.base.thread.three;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author 十三月之夜
 */
public class ReadWriteLockLearn {

    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static void main(String[] args) {
        MyThread readThread = new MyThread(true);
        MyThread writeThread = new MyThread(false);
        for (int i = 0; i < 20; ++ i) {
            if (i % 10 == 0) {
                new Thread(writeThread, i + "").start();
            } else {
                new Thread(readThread, i + "").start();
            }
        }
    }

    private static class MyThread implements Runnable {

        private static String value = UUID.randomUUID().toString();

        private boolean isRead;

        public MyThread(boolean isRead) {
            this.isRead = isRead;
        }

        @Override
        public void run() {
            if (isRead) {
                readWriteLock.readLock().lock();
                try {
                    System.out.println("模拟耗时操作...");
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    ;
                }
                System.out.println(Thread.currentThread().getName() + "读取到: " + value);
                readWriteLock.readLock().unlock();
            } else {
                readWriteLock.writeLock().lock();
                try {
                    System.out.println("模拟耗时操作...");
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    ;
                }
                value = UUID.randomUUID().toString();
                System.out.println(Thread.currentThread().getName() + "更新了: " + value);
                readWriteLock.writeLock().unlock();
            }
        }
    }
}
