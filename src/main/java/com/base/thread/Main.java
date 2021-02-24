package com.base.thread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * @author 十三月之夜
 */
public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        FutureTask0<String> futureTask = new FutureTask0<>(() -> {
            return UUID.randomUUID().toString();
        });
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(futureTask);
    }

    private static class FutureTask0<T> extends FutureTask<T> {

        public FutureTask0(Callable<T> callable) {
            super(callable);
        }

        @Override
        protected void done() {
            try {
                System.out.println("Done: " + get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
