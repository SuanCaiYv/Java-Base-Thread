package com.base.thread.five;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 十三月之夜
 */
public class ProducerConsumerLearn {

    public static void main(String[] args) {
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(50);
        TestProducer testProducer = new TestProducer(blockingQueue);
        TestConsumer testConsumer = new TestConsumer(blockingQueue);
        for (int i = 0; i < 10; ++ i) {
            new Thread(testProducer).start();
        }
        for (int i = 0; i < 5; ++ i) {
            new Thread(testConsumer).start();
        }
    }

    private static class TestProducer implements Runnable {

        private final BlockingQueue<String> blockingQueue;

        public TestProducer(BlockingQueue<String> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        @Override
        public void run() {
            Random random = new Random();
            for (int i = 0; i < 5; ++ i) {
                LockSupport.parkNanos(random.nextInt(100) * 1000 * 1000);
                try {
                    String value = UUID.randomUUID().toString();
                    System.out.println(Thread.currentThread().getName() + ": put a value: " + value);
                    blockingQueue.put(value);
                } catch (InterruptedException ignored) {
                    ;
                }
            }
        }
    }

    private static class TestConsumer implements Runnable {

        private final BlockingQueue<String> blockingQueue;

        public TestConsumer(BlockingQueue<String> blockingQueue) {
            this.blockingQueue = blockingQueue;
        }

        @Override
        public void run() {
            Random random = new Random();
            for (int i = 0; i < 10; ++ i) {
                LockSupport.parkNanos(random.nextInt(100) * 1000 * 1000);
                try {
                    String value = blockingQueue.take();
                    System.out.println(Thread.currentThread().getName() + ": get a value: " + value);
                } catch (InterruptedException ignored) {
                    ;
                }
            }
        }
    }
}
