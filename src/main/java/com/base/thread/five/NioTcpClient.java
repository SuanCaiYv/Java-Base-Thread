package com.base.thread.five;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

/**
 * @author 十三月之夜
 */
public class NioTcpClient {

    public static void main(String[] args) throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8190));
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 5);
        for (int i = 0; i < 10; ++ i) {
            String uuid = UUID.randomUUID().toString();
            socketChannel.write(ByteBuffer.wrap(uuid.getBytes(StandardCharsets.UTF_8)));
            byteBuffer.clear();
            int readable = socketChannel.read(byteBuffer);
            System.out.println(new String(byteBuffer.array(), 0, readable));
        }
        socketChannel.shutdownInput();
        socketChannel.shutdownOutput();
        socketChannel.close();
        // new Scanner(System.in).nextLine();
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("请输入:");
//        String str = scanner.nextLine();
//        while (!str.equals("exit")) {
//            socketChannel.write(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
//            byteBuffer.clear();
//            int readable = socketChannel.read(byteBuffer);
//            System.out.println(new String(byteBuffer.array(), 0, readable));
//            System.out.println("请输入:");
//            str = scanner.nextLine();
//        }
    }
}
