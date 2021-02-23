package com.base.thread.five;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author 十三月之夜
 */
public class NioTcpSingleThread {

    public static void main(String[] args) {
        NioTcpSingleThread.Server.builder().build().run();
    }

    private static final HashMap<SocketChannel, DataLoad> dataLoads = new LinkedHashMap<>();

    private static final ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 5);

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
     * Java NIO处理网络的核心组件只有四个：{@link Channel}，{@link Selector}，{@link SelectionKey}和{@link java.nio.Buffer}
     * <br/>
     * 说一下{@link ServerSocketChannel}，{@link SocketChannel}，{@link Selector}和{@link SelectionKey}之间的关系。
     * <br/>
     * {@link ServerSocketChannel}和{@link SocketChannel}不说了，无非就是一个用来在服务端建立连接，一个处理连接(实际I/O交互)的区别，在这里统称为{@link AbstractSelectableChannel}，也就是它俩都继承的类。
     * <br/>
     * {@link Selector#select()}调用系统调用，轮询端口，记录已注册的{@link AbstractSelectableChannel}感兴趣的事件，如果发生了所有已注册的{@link AbstractSelectableChannel}感兴趣的事件之一的话，就返回。否则阻塞。
     * <br/>
     * 对于{@link AbstractSelectableChannel}来说，怎么让{@link Selector}帮自己记录并轮询自己感兴趣的事件呢？答案是：注册到{@link Selector}上即可，同时设置感兴趣的事件类型。
     * <br/>
     * 在注册成功后，会返回一个{@link SelectionKey}类型的变量，通过它，可以操作{@link AbstractSelectableChannel}和{@link Selector}。{@link SelectionKey}本身就是{@link AbstractSelectableChannel}和它注册到的{@link Selector}的凭证。
     * 就像是订单一样，记录着它们俩的关系，所以在注册成功的后续操作里，一般都是用{@link SelectionKey}来实现的。同时，{@link SelectionKey}还有一个attachment()方法，可以获取附加到它上面的对象。
     * 一般我们用这个附属对象来处理当前{@link SelectionKey}所包含的{@link AbstractSelectableChannel}和{@link Selector}的实际业务。
     * <br/>
     * 刚才说到了{@link Selector#select()}，它会一直阻塞直到发生了感兴趣的事件，但是有时候我们这边可以确定某一事件马上或已经发生，就可以调用{@link Selector#wakeup()}方法，让{@link Selector#select()}立即返回，然后获取
     * {@link SelectionKey}集合也好，重新{@link Selector#select()}(这已经是下一次循环了)也罢。
     * <br/>
     * <br/>
     * 注意！！！如果某一个{@link AbstractSelectableChannel}在同一个{@link Selector}上注册了两个不同的感兴趣的事件类型，那么返回的两个{@link SelectionKey}是没有任何关系的。虽然可以通过{@link SelectionKey}再次修改
     * {@link AbstractSelectableChannel}感兴趣的事件类型。{@link SelectionKey}只在注册时生成返回，所以有(Channel + Selector) = SelectionKey。但是吧，啧，注册多个时会卡死，所以千万不要同一个Channel和同一个Selector注册多个！！！
     */
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
                        .globalSelector(globalSelector)
                        .serverSocketChannel(serverSocketChannel)
                        .build()
                );
                while (true) {
                    // select()是正儿八经的阻塞方法，它会一直阻塞直到发生了任何注册过的(Server)SocketChannel感兴趣的事件之一。比如有新的连接建立，Channel可以读了，或者Channel可以写了
                    // 它的返回值指出了有几个感兴趣事件，实际没啥用，所以在此直接忽略
                    globalSelector.select();
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

        private final Selector globalSelector;

        private final ServerSocketChannel serverSocketChannel;

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                System.out.println("已建立连接...");
                socketChannel.configureBlocking(false);
                SelectionKey socketSelectionKey = socketChannel.register(globalSelector, SelectionKey.OP_READ);
                socketSelectionKey.attach(Handler.builder()
                        .socketSelectionKey(socketSelectionKey)
                        .build()
                );
                // 此时注册了读感兴趣Channel，所以为了快速开启读，直接唤醒selector。其实就是让它别等了，我这边准备好了，你那边应该已经有数据了，直接返回吧。
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
            SocketChannel socketChannel = (SocketChannel) socketSelectionKey.channel();
            if (!socketSelectionKey.isValid()) {
                try {
                    socketChannel.close();
                } catch (IOException ignored) {
                    ;
                }
            }
            if (socketSelectionKey.isReadable()) {
                System.out.println("读事件发生，准备读...");
                Reader.builder()
                        .socketChannel(socketChannel)
                        .build()
                        .run();
                // 说明即对读感兴趣，也对写感兴趣(因为客户端可能是长连接，还要再次发送消息)，但是同一个SelectionKey只能是读或写之一
                socketSelectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                // 读完了，就要准备写
                socketSelectionKey.selector().wakeup();
            }
            if (socketSelectionKey.isWritable()) {
                Writer.builder()
                        .socketChannel(socketChannel)
                        .build()
                        .run();
                socketSelectionKey.interestOps(SelectionKey.OP_READ);
                // 写完了，立即返回就免了
                // socketSelectionKey.selector().wakeup();
            }
        }
    }

    @Data
    @Builder
    private static class Reader implements Runnable {

        private final SocketChannel socketChannel;

        @Override
        public void run() {
            try {
                byteBuffer.clear();
                int readable = socketChannel.read(byteBuffer);
                byte[] bytes = byteBuffer.array();
                String value = new String(bytes, 0, readable);
                System.out.println("读到了: " + value);
                DataLoad dataLoad = DataLoad.builder()
                        .stringValue(value)
                        .build();
                dataLoads.put(socketChannel, dataLoad);
            } catch (IOException ignored) {
            }
        }
    }

    @Data
    @Builder
    private static class Writer implements Runnable {

        private final SocketChannel socketChannel;

        @Override
        public void run() {
            try {
                System.out.println("写事件发生，准备写...");
                String value = "Server get: " + dataLoads.get(socketChannel).getStringValue();
                socketChannel.write(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
            } catch (IOException ignored) {
                ;
            }
        }
    }
}
