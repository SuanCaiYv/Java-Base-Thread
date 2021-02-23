package com.base.thread;

import java.nio.channels.SelectionKey;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntConsumer;

/**
 * @author 十三月之夜
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(SelectionKey.OP_READ);
        System.out.println(SelectionKey.OP_WRITE);
        System.out.println(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }
}
