package com.learn.tools;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 压测机器的并发线程数量程序
 */
public class TestThread extends Thread {
    private static final AtomicInteger count = new AtomicInteger();

    public static void main(String[] args) {
        while (true)
            (new TestThread()).start();
    }

    @Override
    public void run() {
        System.out.println(count.incrementAndGet());

        while (true)
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                break;
            }
    }
}
