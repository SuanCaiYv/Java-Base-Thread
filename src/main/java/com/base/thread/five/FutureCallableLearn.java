package com.base.thread.five;

import java.util.UUID;
import java.util.concurrent.*;

/**
 * @author 十三月之夜
 */
public class FutureCallableLearn {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Callable<String> callable = () -> {
            return UUID.randomUUID().toString();
        };
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        FutureTask<String> futureTask = new FutureTask<>(callable);
        Future<String> future = executorService.submit(callable);
        executorService.submit(futureTask);
        System.out.println(future.get());
        System.out.println(futureTask.get());
        executorService.shutdown();
    }
}
