package com.learn._03_designPattern;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Thread-Per-Message 设计模式：并发编程领域里，解决分工问题的一种设计模式。
 *
 * 需求：
 *  - 网络编程里服务端的实现，服务端为每个客户端请求创建一个独立的线程，当线程处理完请求后，自动销毁。
 *  - 如果在主线程中处理 HTTP 请求的话，那同一时间只能处理一个请求，太慢了，
 *    可以利用代办的思路，创建一个子线程，委托子线程去处理 HTTP 请求，
 *    当线程处理完请求后，自动销毁线程。
 * 模式缺点：
 *  - 该模式的缺点是，Java 中线程和操作系统线程是一一对应的。
 *    所以Java 线程是一个重量级的对象，创建成本很高。
 *    一方面是创建线程比较耗时，另一方面线程占用的内容也很大。
 *    所以为每个请求创建一个新线程的这种设计模式并不适合高并发的场景。
 *  解决方案：
 *  - 解决方案可以选择引入线程池的方式优化，但会导致程序的复杂度太高。
 *  - 还有一种解决方法就是使用轻量级线程，但是 Java 语言天生并不支持轻量级线程，
 *    所以其他语言例如 Go 语言，Lua 语言中的协程，就发挥作用了，
 *    其实协程本质也是一种轻量级的线程，由语言自身维护创建和调度。
 *    轻量级的线程的特点是创建成本低，基本上和创建一个普通对象的成本相似，
 *    创建的速度和内存占用相比于操作系统的线程至少有一个数量级的提升。
 */
public class _06_ThreadPerMessage {

    public static void main(String[] args) throws IOException {
        final ServerSocketChannel ssc =
                ServerSocketChannel.open().bind(new InetSocketAddress(8080));
        // 处理请求
        try {
            while (true){
                // 接受请求
                SocketChannel sc = ssc.accept();
                // 为每个请求创建一个线程处理
                new Thread(()->{
                    try {
                        // 读 Socket
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                        sc.read(readBuffer);
                        // 模拟处理请求
                        Thread.sleep(2000);
                        // 写 Socket
                        ByteBuffer writeBuffer = (ByteBuffer)readBuffer.flip();
                        sc.write(writeBuffer);
                        // 关闭 Socket
                        sc.close();
                    }catch (Exception e){
                        throw new UncheckedIOException((IOException) e);
                    }
                }).start();
            }
        }finally {
            ssc.close();
        }
    }
}
