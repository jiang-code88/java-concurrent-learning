package com.learn._03_designPattern;

import java.util.concurrent.*;

/**
 * 为了有效避免线程的频繁创建、销毁以及 OOM 问题。
 * 使用 Worker Thread 模式可以实现对线程的有效复用，限制创建线程的上限。
 *
 * - 线程池就是典型的 Worker Thread 模式实现
 * 1. 正确的创建线程池
 * 2. 线程池使用中可能出现的死锁问题
 */
public class _07_WorkerThread {
    public static void main(String[] args) throws InterruptedException {
        // 模拟使用线程池可能出现的死锁问题
        // ThreadPoolDeadLock();
        // 对不用的任务拆分使用不同的线程池解决线程池可能出现的死锁问题
        ThreadPoolDeadLock_A();
    }

    // 正确的创建线程池
    public static void correctlyCreateThreadPool(){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(50, 500, 60L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(2000),
                // 1.创建线程池时，创建有界队列来接受任务，避免无限制接受任务导致 OOM
                r -> {
                    return new Thread(r, "echo" + r.hashCode());
                },
                // 2.为了便于调试和诊断问题，给每个线程赋予一个业务相关的名字
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    // 使用线程池可能出现的死锁问题
    // - 如下例子将永远执行不到输出 "l1 end" 这句语句，线程池的所有线程都阻塞在 l2Latch.await(); 语句上。
    // - 如果提交到相同线程池的任务不是相互独立的，而是有依赖关系的，那么就有可能导致线程死锁。
    // - 所有提交到相同线程池中的任务一定是相互独立的，否则要为不同的任务创建不同的线程池。
    // TODO：尝试使用工具分析程序的线程栈信息，分析出线程池中所有的线程都阻塞到某个位置。
    public static void ThreadPoolDeadLock() throws InterruptedException {
        // L1 和 L2 阶段公用的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        // L1 阶段的计数器
        CountDownLatch l1Latch = new CountDownLatch(2);
        // 执行 L1 阶段任务
        for (int i = 0; i < 2; i++) {
            int finalI = i;
            executorService.execute(()->{
                System.out.println("L1-" + finalI);
                // L2 阶段的计数器
                CountDownLatch l2Latch = new CountDownLatch(2);
                // 执行 L1 阶段的 L2 阶段子任务
                for (int j = 0; j < 2; j++) {
                    int finalJ = j;
                    executorService.execute(()->{
                        System.out.println("L2-" + finalJ);
                        l2Latch.countDown();
                    });
                }
                // 等待 L2 阶段任务执行完
                try {
                    l2Latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("l2 end");
                l1Latch.countDown();
            });
        }
        // 等待 L1 阶段任务执行完
        l1Latch.await();
        System.out.println("l1 end");
        executorService.shutdown();
    }

    // 为不同的任务创建不同的线程池，解决线程池使用的死锁问题
    public static void ThreadPoolDeadLock_A() throws InterruptedException {
        // L1 和 L2 阶段公用的线程池
        ExecutorService l1es = Executors.newFixedThreadPool(2);
        ExecutorService l2es = Executors.newFixedThreadPool(2);
        // L1 阶段的计数器
        CountDownLatch l1Latch = new CountDownLatch(2);
        // 执行 L1 阶段任务
        for (int i = 0; i < 2; i++) {
            int finalI = i;
            l1es.execute(()->{
                System.out.println("L1-" + finalI);
                // L2 阶段的计数器
                CountDownLatch l2Latch = new CountDownLatch(2);
                // 执行 L1 阶段的 L2 阶段子任务
                for (int j = 0; j < 2; j++) {
                    int finalJ = j;
                    l2es.execute(()->{
                        System.out.println("L1-" + finalI + "->L2-" + finalJ);
                        l2Latch.countDown();
                    });
                }
                // 等待 L2 阶段任务执行完
                try {
                    l2Latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("L1-" + finalI + "->L2 end");
                l1Latch.countDown();
            });
        }
        // 等待 L1 阶段任务执行完
        l1Latch.await();
        System.out.println("L1 end");
        l2es.shutdown();
        l1es.shutdown();
    }

}
