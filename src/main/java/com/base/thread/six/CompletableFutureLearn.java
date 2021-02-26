package com.base.thread.six;

import lombok.Builder;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 十三月之夜
 */
public class CompletableFutureLearn {

    public static void main(String[] args) {
        CompletableFuture<String> completableFuture1 = new CompletableFuture<>();
        System.out.println("now1: " + format.format(System.currentTimeMillis()) + "Thread: " + Thread.currentThread().getName());
        TestClassOne testClassOne = new TestClassOne(completableFuture1);
        new Thread(testClassOne).start();
        LockSupport.parkNanos(100 * 1000 * 1000);
        // 手动complete来填充结果
        completableFuture1.complete(UUID.randomUUID().toString());
        System.out.println("now2: " + format.format(System.currentTimeMillis()) + "Thread: " + Thread.currentThread().getName());
        // 流式处理，拼接多个ComtableFuture
        CompletableFuture.supplyAsync(() -> {
            // 模拟耗时操作
            LockSupport.parkNanos(100 * 1000 * 1000);
            return UUID.randomUUID().toString();
        }).thenAccept(uuid -> {
            System.out.println("now2: " + format.format(System.currentTimeMillis()) + "Thread: " + Thread.currentThread().getName() + " get uuid: " + uuid);
        });
        System.out.println("now2: " + format.format(System.currentTimeMillis()) + "Thread: " + Thread.currentThread().getName());
        // 处理异常
        CompletableFuture.supplyAsync(() -> {
            throw new NullPointerException();
        }).exceptionallyAsync(e -> {
            return "get: " + e.getMessage();
        }).thenAccept(System.out::println);
        LockSupport.parkNanos(Long.MAX_VALUE);
    }

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS ");

    @Data
    private static class TestClassOne implements Runnable {

        private CompletableFuture<String> completableFuture;

        public TestClassOne(CompletableFuture<String> completableFuture) {
            this.completableFuture = completableFuture;
        }

        @Override
        public void run() {
            try {
                String str = completableFuture.get();
                System.out.println("now1: " + format.format(System.currentTimeMillis()) + "Thread: " + Thread.currentThread().getName() + " get: " + str);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
