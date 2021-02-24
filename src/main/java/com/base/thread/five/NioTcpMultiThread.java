package com.base.thread.five;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 十三月之夜
 */
public class NioTcpMultiThread {

    public static void main(String[] args) {
        Server.builder().build().run();
        Runnable target = executorService::shutdown;
        Thread shutdown = new Thread(target);
        Runtime.getRuntime().addShutdownHook(shutdown);
    }

    private static final ConcurrentHashMap<SocketChannel, LinkedBlockingQueue<DataLoad>> dataLoads = new ConcurrentHashMap<>();

    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 4);

    private static final ReentrantLock reentrantLock = new ReentrantLock();

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
            System.out.println("Server开始运行...");
            Selector globalSelector;
            ServerSocketChannel serverSocketChannel;
            SelectionKey serverSelectionKey;
            try {
                globalSelector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(8190));
                serverSocketChannel.configureBlocking(false);
                serverSelectionKey = serverSocketChannel.register(globalSelector, SelectionKey.OP_ACCEPT);
                serverSelectionKey.attach(Acceptor.builder()
                        .serverSelectionKey(serverSelectionKey)
                        .build()
                );
                while (true) {
                    int a = globalSelector.select();
                    Set<SelectionKey> selectionKeySet = globalSelector.selectedKeys();
                    for (SelectionKey selectionKey : selectionKeySet) {
                        dispatch(selectionKey);
                        selectionKeySet.remove(selectionKey);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        private void dispatch(SelectionKey selectionKey) {
            Runnable runnable = (Runnable) selectionKey.attachment();
            runnable.run();
        }
    }

    @Data
    @Builder
    private static class Acceptor implements Runnable {

        private final SelectionKey serverSelectionKey;

        @Override
        public void run() {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) serverSelectionKey.channel();
            Selector globalSelector = serverSelectionKey.selector();
            SocketChannel socketChannel;
            try {
                socketChannel = serverSocketChannel.accept();
                System.out.println("已建立连接...");
                socketChannel.configureBlocking(false);
                SelectionKey socketSelectionKey = socketChannel.register(globalSelector, SelectionKey.OP_READ);
                socketSelectionKey.attach(Handler.builder()
                        .socketSelectionKey(socketSelectionKey)
                        .build()
                );
                globalSelector.wakeup();
            } catch (IOException ignored) {
            }
        }
    }

    @Data
    @Builder
    private static class Handler implements Runnable {

        private final SelectionKey socketSelectionKey;

        @Override
        public void run() {
            if (!socketSelectionKey.channel().isOpen()) {
                System.out.println("连接已关闭");
                try {
                    socketSelectionKey.channel().close();
                } catch (IOException e) {
                    ;
                }
                return ;
            }
            dataLoads.computeIfAbsent((SocketChannel) socketSelectionKey.channel(), k -> new LinkedBlockingQueue<>());
            if (socketSelectionKey.isReadable()) {
                Reader reader = Reader.builder()
                        .socketSelectionKey(socketSelectionKey)
                        .build();
                Thread thread = new Thread(reader);
                socketSelectionKey.interestOps(SelectionKey.OP_WRITE);
                thread.start();
            } else if (socketSelectionKey.isWritable()) {
                Writer writer = Writer.builder()
                        .socketSelectionKey(socketSelectionKey)
                        .build();
                Thread thread = new Thread(writer);
                socketSelectionKey.interestOps(SelectionKey.OP_READ);
                thread.start();
            }
        }
    }

    @Data
    @Builder
    private static class Reader implements Runnable {

        private final SelectionKey socketSelectionKey;

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = (SocketChannel) socketSelectionKey.channel();
                String value = null;
                reentrantLock.lock();
                int readable = socketChannel.read(byteBuffer);
                if (readable == 0) {
                    System.out.println("读到空请求");
                } else {
                    value = new String(byteBuffer.array(), 0, readable);
                }
                reentrantLock.unlock();
                if (value == null) {
                    return ;
                }
                System.out.println("读到了: " + value);
                DataLoad dataLoad = DataLoad.builder()
                        .stringValue(value)
                        .build();
                LinkedBlockingQueue<DataLoad> tmp = dataLoads.computeIfAbsent(socketChannel, k -> new LinkedBlockingQueue<>());
                tmp.add(dataLoad);
                socketSelectionKey.selector().wakeup();
            } catch (IOException ignored) {
            }
        }
    }

    @Data
    @Builder
    private static class Writer implements Runnable {

        private final SelectionKey socketSelectionKey;

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = (SocketChannel) socketSelectionKey.channel();
                LinkedBlockingQueue<DataLoad> queue = dataLoads.get(socketChannel);
                String value = "Server get: " + dataLoads.get(socketChannel).take().getStringValue();
                socketChannel.write(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
            } catch (IOException | InterruptedException ignored) {
            }
        }
    }
}
