package com.base.thread.five;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 十三月之夜
 */
public class AioTcpSingleThread {

    public static void main(String[] args) {
        Server.builder().build().run();
        // 防止主线程退出
        LockSupport.park(Long.MAX_VALUE);
    }

    private static final ConcurrentHashMap<AsynchronousSocketChannel, LinkedBlockingQueue<DataLoad>> dataLoads = new ConcurrentHashMap<>();

    private static final ReentrantLock READ_LOCK = new ReentrantLock();

    private static final ReentrantLock WRITE_LOCK = new ReentrantLock();

    private static final ByteBuffer READ_BUFFER = ByteBuffer.allocate(1024 * 4);

    private static final ByteBuffer WRITE_BUFFER = ByteBuffer.allocate(1024 * 4);

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

    @Builder
    private static class Server implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("服务器启动...");
                asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
                asynchronousServerSocketChannel.bind(new InetSocketAddress(8190));
                asynchronousServerSocketChannel.accept(null, ACCEPTOR);
            } catch (IOException ignored) {
            }
        }
    }

    private static AsynchronousServerSocketChannel asynchronousServerSocketChannel = null;

    private static final Acceptor ACCEPTOR = new Acceptor();

    private static class Acceptor implements CompletionHandler<AsynchronousSocketChannel, Object> {
        // 这个方法是异步调用的，所以不用担心阻塞会阻塞到主线程
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            System.out.println("连接建立: " + Thread.currentThread().getName());
            System.out.println("连接建立");
            dataLoads.computeIfAbsent(result, k -> new LinkedBlockingQueue<>());
            // 使用循环来进行多次读取，写入
            while (result.isOpen()) {
                READ_LOCK.lock();
                // 这个方法也是异步的
                result.read(READ_BUFFER, attachment, new Reader(result, READ_BUFFER.array()));
                READ_BUFFER.clear();
                READ_LOCK.unlock();
                WRITE_LOCK.lock();
                String ans = "";
                try {
                    ans = "Server get: " + dataLoads.get(result).take().getStringValue();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 异步的
                result.write(ByteBuffer.wrap(ans.getBytes(StandardCharsets.UTF_8)), attachment, new Writer(result));
                WRITE_LOCK.unlock();
            }
            System.out.println("结束通信一次");
            // 尝试建立第二波通信
            asynchronousServerSocketChannel.accept(attachment, ACCEPTOR);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("建立连接失败");
        }
    }

    private static class Reader implements CompletionHandler<Integer, Object> {

        private final AsynchronousSocketChannel asynchronousSocketChannel;

        private final byte[] bytes;

        public Reader(AsynchronousSocketChannel asynchronousSocketChannel, byte[] bytes) {
            this.asynchronousSocketChannel = asynchronousSocketChannel;
            this.bytes = bytes;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            System.out.println("读取数据: " + Thread.currentThread().getName());
            if (result == 0 || !asynchronousSocketChannel.isOpen()) {
                return ;
            } else if (result < 0) {
                shutdown(asynchronousSocketChannel);
                return ;
            }
            System.out.println("读取数据: " + result);
            String value = new String(bytes, 0, result);
            System.out.println("读到了: " + value);
            LinkedBlockingQueue<DataLoad> tmp = dataLoads.get(asynchronousSocketChannel);
            DataLoad dataLoad = DataLoad.builder()
                    .stringValue(value)
                    .build();
            tmp.add(dataLoad);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("读取失败");
        }
    }

    private static class Writer implements CompletionHandler<Integer, Object> {

        private final AsynchronousSocketChannel asynchronousSocketChannel;

        public Writer(AsynchronousSocketChannel asynchronousSocketChannel) {
            this.asynchronousSocketChannel = asynchronousSocketChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            System.out.println("写入数据: " + Thread.currentThread().getName());
            if (!asynchronousSocketChannel.isOpen()) {
                return ;
            }
            System.out.println("写入数据: " + result);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("写入失败");
        }
    }

    private static void shutdown(AsynchronousSocketChannel asynchronousSocketChannel) {
        try {
            asynchronousSocketChannel.shutdownInput();
            asynchronousSocketChannel.shutdownOutput();
            asynchronousSocketChannel.close();
        } catch (IOException ignore) {
        }
    }
}
