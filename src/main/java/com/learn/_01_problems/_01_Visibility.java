package com.learn._01_problems;

/**
 * 重现出多线程的可见性问题：可见性问题会导致下述例子运行结果为一亿
 * 分析:
 *  代码 (1) 处的共享变量会被 (2)、(3) 两处开启的两个线程加载到自己的 CPU 缓存中,
 *  导致两个线程各自的操作对于对方来说都是不可见的，线程各自将 count 变量从 0 加到 一亿 后再写回内存中,
 *  导致实际的计算结果只有一亿, 不符合我们编写程序时预期的两亿结果 (实际运行是一亿多一点是因为两个线程启动并不是同时的，存在一点时间差)
 * 解决方案:
 *  对线程共享变量 count 使用 volatile 关键字修饰 (3), 表示对这个共享变量的读写禁用 CPU 缓存, 防止可见性问题。
 *  但是使用 volatile 关键字后结果有可能并不是我们期望的两亿, 而是小于并接近两亿(超过1亿5的), 这其实是原子性问题(线程切换)所导致的。
 */
public class _01_Visibility {
    // 线程共享变量
    // private static int count = 0; // (1)
    private static volatile int count = 0; // (3)

    // 对共享变量进行加到一个亿的操作
    private static void addOneHundredMillion(){
        int idx = 0;
        while (idx++ < 100000000){
            count += 1;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 不断开启多线程(两个线程)同时操作共享变量
        while (true){
            // 两个线程同时执行对共享变量加到一个亿的操作
            // 期望的合理结果是共享变量 count 的结果为 两亿
            Thread thread1 = new Thread(() -> {  // (2)
                addOneHundredMillion();
            });
            Thread thread2 = new Thread(() -> {  // (3)
                addOneHundredMillion();
            });
            // 启动两个线程
            thread1.start();
            thread2.start();
            // 主线程等待两个子线程结束后再继续执行
            thread1.join();
            thread2.join();
            // 打印两个线程并发操作的共享变量结果
            System.out.println("result: [" + count + "]");
            // 如果结果不为两亿, 不再运行程序
            if (count != 200000000){
                break;
            }
            // 重置结果, 供下一次多线程操作结果展示使用
            count = 0;
        }
    }

}

