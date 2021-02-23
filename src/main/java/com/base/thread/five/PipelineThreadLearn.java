package com.base.thread.five;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 十三月之夜
 */
public class PipelineThreadLearn {

    private static final Semaphore firstSemaphore = new Semaphore(1);

    private static final Semaphore secondSemaphore = new Semaphore(0);

    private static final Semaphore thirdSemaphore = new Semaphore(0);

    private static final DataLoad dataLoad = new DataLoad();

    public static void main(String[] args) {
        ThreadFirst threadFirst = new ThreadFirst();
        ThreadSecond threadSecond = new ThreadSecond();
        ThreadThird threadThird = new ThreadThird();
        dataLoad.setIntArray(new int[] {12, 24});
        dataLoad.setDoubleValue(144);
        Thread thread1 = new Thread(threadFirst);
        Thread thread2 = new Thread(threadSecond);
        Thread thread3 = new Thread(threadThird);
        thread1.start();
        thread2.start();
        thread3.start();
        LockSupport.parkNanos(500 * 1000 * 1000);
        thread1.interrupt();
        thread2.interrupt();
        thread3.interrupt();
        System.out.println(dataLoad.getStringValue());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class DataLoad {
        private int intValue;

        private long longValue;

        private double doubleValue;

        private String stringValue;

        private int[] intArray;

        private long[] longArray;

        private double[] doubleArray;

        private String[] stringArray;
    }

    private static class ThreadFirst implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    firstSemaphore.acquire();
                    long ans = dataLoad.getIntArray()[0] + dataLoad.getIntArray()[1];
                    dataLoad.setLongValue(ans);
                    secondSemaphore.release();
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    private static class ThreadSecond implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    secondSemaphore.acquire();
                    double ans = dataLoad.getDoubleValue() / dataLoad.getLongValue();
                    dataLoad.setDoubleValue(ans);
                    thirdSemaphore.release();
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    private static class ThreadThird implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    thirdSemaphore.acquire();
                    dataLoad.setStringValue("Ans is: " + dataLoad.getDoubleValue());
                    firstSemaphore.release();
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }
}
