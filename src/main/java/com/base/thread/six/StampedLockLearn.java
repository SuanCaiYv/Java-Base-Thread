package com.base.thread.six;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;

/**
 * @author 十三月之夜
 */
public class StampedLockLearn {

    public static void main(String[] args) {
        ReaderOne readerOne = new ReaderOne();
        WriterOne writerOne = new WriterOne();
        for (int i = 0; i < 10; ++ i) {
            new Thread(readerOne).start();
            new Thread(writerOne).start();
        }
    }

    private static final StampedLock stampedLock = new StampedLock();

    private static String uuid;

    private static class ReaderOne implements Runnable {

        @Override
        public void run() {
            int random = new Random().nextInt(500);
            LockSupport.parkNanos(random * 1000 * 1000);
            // 先乐观地读
            long stamp = stampedLock.tryOptimisticRead();
            String localUuid = uuid;
            // 如果发现乐观读失败
            if (!stampedLock.validate(stamp)) {
                // 进行悲观地读
                stamp = stampedLock.readLock();
                localUuid = uuid;
                // 释放悲观锁
                stampedLock.unlockRead(stamp);
            }
            System.out.println("Reader " + Thread.currentThread().getName() + " Get: " + localUuid);
        }
    }

    private static class WriterOne implements Runnable {

        @Override
        public void run() {
            int random = new Random().nextInt(500);
            LockSupport.parkNanos(random * 1000 * 1000);
            long stamp = stampedLock.writeLock();
            uuid = UUID.randomUUID().toString();
            stampedLock.unlockWrite(stamp);
        }
    }
}
