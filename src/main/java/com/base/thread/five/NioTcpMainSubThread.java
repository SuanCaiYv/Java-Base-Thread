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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 十三月之夜
 */
public class NioTcpMainSubThread {

    public static void main(String[] args) throws IOException {
        new Server(Runtime.getRuntime().availableProcessors()).run();
    }

    private static final ConcurrentHashMap<SocketChannel, LinkedBlockingQueue<DataLoad>> dataLoads = new ConcurrentHashMap<>();

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

    /**
     * BossSelector只和ServerSocketChannel进行建立连接操作，且是单线程的，运行在主线程中。
     * <br/>
     * 然后把建立的连接扔给Workers处理，Workers是一组Worker，每个Worker都有一个独立的WorkSelector用来处理当前Worker被安排的SocketChannel。
     * <br/>
     * 这里采取的策略是依次提交，尽可能让每个Worker所负责的SocketChannel数量相同。
     * <br/>
     * 每个Worker运行在独立的线程上，仅做轮询Read/Write操作，耗时的业务操作(比如I/O，Compute)均交给线程工作池处理。
     */
    private static class Server implements Runnable {

        private final Selector bossSelector;

        private final int workerCount;

        public Server(int workerCount) throws IOException {
            this.workerCount = workerCount;
            bossSelector = Selector.open();
        }

        @Override
        public void run() {
            try {
                System.out.println("服务器启动...");
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(8190));
                serverSocketChannel.configureBlocking(false);
                SelectionKey serverSelectionKey = serverSocketChannel.register(bossSelector, SelectionKey.OP_ACCEPT);
                serverSelectionKey.attach(new Boss(serverSocketChannel, workerCount));
                while (true) {
                    bossSelector.select();
                    Set<SelectionKey> selectionKeySet = bossSelector.selectedKeys();
                    // 特殊化处理，因为有且只有一个SelectionKey，所以不遍历了
                    SelectionKey key = selectionKeySet.iterator().next();
                    Runnable runnable = (Runnable) key.attachment();
                    runnable.run();
                    selectionKeySet.remove(key);
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 处理新的连接，生成SocketChannel并选择某一个Worker提交
     */
    private static class Boss implements Runnable {

        private final ServerSocketChannel serverSocketChannel;

        private final int workerCount;

        private final Set<SocketChannel>[] socketChannelSets;

        private final Worker[] workers;

        private int index = 0;

        @SuppressWarnings("unchecked")
        public Boss(ServerSocketChannel serverSocketChannel, int workerCount) throws IOException {
            this.serverSocketChannel = serverSocketChannel;
            this.workerCount = workerCount;
            ExecutorService executorService = Executors.newFixedThreadPool(workerCount);
            socketChannelSets = new Set[workerCount];
            workers = new Worker[workerCount];
            for (int i = 0; i < workerCount; ++ i) {
                workers[i] = new Worker();
                socketChannelSets[i] = workers[i].getSocketChannels();
                executorService.submit(workers[i]);
            }
        }

        @Override
        public void run() {
            Set<SocketChannel> socketChannelSet = socketChannelSets[index];
            Selector workerSelector = workers[index].getWorkerSelector();
            ++ index;
            if (index == this.workerCount)
                index = 0;
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                System.out.println("建立连接...");
                socketChannelSet.add(socketChannel);
                workerSelector.wakeup();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * 处理新添加的SocketChannel和轮询Read/Write。
     */
    private static class Worker implements Runnable {

        private final Selector workerSelector;

        private final Set<SocketChannel> socketChannels = new HashSet<>();

        public Worker() throws IOException {
            workerSelector = Selector.open();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (socketChannels.size() > 0) {
                        for (SocketChannel socketChannel : socketChannels) {
                            socketChannel.configureBlocking(false);
                            SelectionKey selectionKey = socketChannel.register(workerSelector, SelectionKey.OP_READ);
                            selectionKey.attach(new Handler(selectionKey));
                            socketChannels.remove(socketChannel);
                        }
                        System.out.println("已添加新的SocketChannel");
                    }
                    workerSelector.select();
                    Set<SelectionKey> selectionKeySet = workerSelector.selectedKeys();
                    for (SelectionKey key : selectionKeySet) {
                        Runnable runnable = (Runnable) key.attachment();
                        runnable.run();
                        selectionKeySet.remove(key);
                    }
                } catch (IOException ignored) {
                }
            }
        }

        public Set<SocketChannel> getSocketChannels() {
            return socketChannels;
        }

        public Selector getWorkerSelector() {
            return workerSelector;
        }
    }

    /**
     * 分发业务处理
     */
    @Data
    @Builder
    private static class Handler implements Runnable {

        private final SelectionKey socketSelectionKey;

        private static final ExecutorService workPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        @Override
        public void run() {
            if (!socketSelectionKey.channel().isOpen()) {
                System.out.println("连接已关闭");
                try {
                    socketSelectionKey.channel().close();
                } catch (IOException ignored) {
                }
                return ;
            }
            dataLoads.computeIfAbsent((SocketChannel) socketSelectionKey.channel(), k -> new LinkedBlockingQueue<>());
            if (socketSelectionKey.isReadable()) {
                Reader reader = Reader.builder()
                        .socketSelectionKey(socketSelectionKey)
                        .build();
                workPool.submit(reader);
                socketSelectionKey.interestOps(SelectionKey.OP_WRITE);
            } else if (socketSelectionKey.isWritable()) {
                Writer writer = Writer.builder()
                        .socketSelectionKey(socketSelectionKey)
                        .build();
                workPool.submit(writer);
                socketSelectionKey.interestOps(SelectionKey.OP_READ);
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
                String value;
                reentrantLock.lock();
                if (socketChannel.isOpen()) {
                    int readable = socketChannel.read(byteBuffer);
                    if (readable == 0) {
                        value = null;
                        // System.out.println("读到空请求");
                    } else if (readable < 0) {
                        value = null;
                        shutdownSocketChannel(socketChannel);
                    } else {
                        value = new String(byteBuffer.array(), 0, readable);
                    }
                } else {
                    value = null;
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
                if (socketChannel.isOpen())
                    socketChannel.write(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
                else {
                    shutdownSocketChannel(socketChannel);
                }
            } catch (IOException | InterruptedException ignored) {
            }
        }
    }

    private static void shutdownSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.shutdownInput();
            socketChannel.shutdownOutput();
            socketChannel.close();
        } catch (IOException ignored) {
        }
    }
}
