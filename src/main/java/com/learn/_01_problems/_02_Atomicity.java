package com.learn._01_problems;

/**
 * 重现出多线程的原子性问题-线程切换导致操作被中断执行
 * 分析:
 *    count += 1 的操作至少需要三条 CPU 指令完成
 *     1. 把变量 count 加载到寄存器。
 *     2. 在寄存器中对变量执行 +1 操作。
 *     3. 将结果写入内存中(禁用了缓存所以不会写入缓存而是直接写入内存)
 *    操作系统时间片轮转的多任务并行，会发生在任何一条 CPU 指令执行完后,
 *    同时保证 CPU 只能保障 CPU 指令级别的原子操作，所以会出现：
 *      -线程A-加载寄存器 0
 *       |线程B-加载寄存器 0
 *       |线程B-执行 +1 操作, 此时为 1
 *       |线程B-将结果 1 写入内存
 *      -线程A-执行 +1 操作, 此时为 1
 *      -线程A-将结果 1 写入内存
 *     两个线程因为 += 1 操作不是原子的, 被中断切换穿插运行, 导致两个线程做了重复的事情, 出现了错误的结果。
 *     错误结果是大于 15000 且小于 20000 的, 因为线程切换毕竟是比较少的, 禁用了缓存就已经好很多了。
 * 解决方案:
 *    使用 synchronized 所将 += 1 操作约束为原子性操作, 避免线程切换对结果的影响。
 */
public class _02_Atomicity {
    // 使用 volatile 关键字禁用 CPU 缓存, 解决可见性问题
    // private static int count = 0;
    private static volatile int count = 0;

    private static void add10K() {
        int idx = 0;
        while (idx++ < 10000) {
            count += 1;
        }
    }

    private static void atomicAdd10K() {
        int idx = 0;
        while (idx++ < 10000) {
            // 使用 synchronized 关键字约束 += 1 操作包含的多个CPU指令为一个原子性操作
            synchronized (_02_Atomicity.class){
                count += 1;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        while (true){
            Thread thread1 = new Thread(() -> {
//                 add10K();
                atomicAdd10K();
            });
            Thread thread2 = new Thread(() -> {
//                 add10K();
                atomicAdd10K();
            });
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
            System.out.println("result: [" + count + "]");
            // 使用 add10K() 也可能等于 20000 也可能不等于, 说明解决可见性已经是线程安全很多了
            // 原子性是必现的但是也只是有一定概率出现而已, 补充之后将完全线程安全了
            if (count != 20000){
                break;
            }
            count = 0;
        }
    }
}

