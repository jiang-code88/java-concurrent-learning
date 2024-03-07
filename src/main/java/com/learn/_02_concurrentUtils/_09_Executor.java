package com.learn._02_concurrentUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 线程池
 *  - 使用线程池的理由：
 *    创建线程并不是在堆中分配一块内存这么简单，而是需要调用操作系统内核 API，
 *    由操作系统为线程分配一系列的资源，是一个重量级的对象，创建成本十分高。
 *    所以建议使用线程池复用已经创建好的线程，避免频繁创建和销毁线程。
 *
 *  - Java 线程池实现采用的是「生产者-消费者」模式
 *    线程池中的线程是消费者，不断消费开发者（生产者）投入线程池的任务。
 *
 *  - 如何使用 Java 中的线程池实现 ThreadServiceExecutor
 *
 *  - 使用 Java 中线程实现的注意事项：
 *    1）尽量避免使用无界队列作为线程池的任务队列。
 *    2）默认拒绝策略要慎重使用。如果线程池处理的任务非常重要，建议自定义自己的拒绝策略；
 *       并且在实际工作中，自定义的拒绝策略往往和降级策略配合使用。
 *    3）虽然线程池提供了很多用于异常处理的方法，但是最稳妥和简单的方案还是捕获所有异常并按需处理。
 *
 *  - 参考《Java 并发编程实战》
 *     第 7 章《取消与关闭》的 7.3 节“处理非正常的线程终止” 详细介绍了异常处理的方案，
 *     第 8 章《线程池的使用》对线程池的使用进行深入的介绍。
 */
public class _09_Executor {
    public static void main(String[] args) {
        /* 1 模拟线程池的使用示例 */

        // 创建有界的阻塞队列
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(2);

        // 创建线程池
        MyThreadPool myThreadPool = new MyThreadPool(3, workQueue);

        // 提交任务，让线程池中的线程主动消费任务
        myThreadPool.execute(()->{
            System.out.println("hello");
        });


        /* 2 如何使用 Java 中的线程池实现 ThreadPoolExecutor */

        // Java 并发包里提供了一个线程池的静态工厂类 Executors，利用 Executors 你可以快速创建线程池。
        // 但是不建议使用 Executors，因为 Executors 提供的很多方法默认使用的都是无界的 LinkedBlockingQueue，
        // 高负载情境下，无界队列很容易导致 OOM，而 OOM 会导致所有请求都无法处理。建议线程池使用有界队列。
        /*
            public ThreadPoolExecutor(
             int corePoolSize,                  // 线程池保有的最小线程数
             int maximumPoolSize,               // 线程池创建的最大线程数
             long keepAliveTime,                // 线程空闲 keepAliveTime & unit 这么久，而且线程池的线程数大于 corePoolSize ，那么这个空闲的线程就要被回收
             TimeUnit unit,                     // 线程空闲时间单位
             BlockingQueue<Runnable> workQueue, // 工作任务队列
             ThreadFactory threadFactory,       // 自定义如何创建线程（可以给线程自定义名字）
             RejectedExecutionHandler handler   // 自定义任务的拒绝策略（如果线程池中所有的线程都在忙碌，并且工作队列也满了（前提是工作队列是有界队列），那么此时提交的任务，使用何种策略处理）
            )
            {
                ...
            }
        */

        /*
           ThreadPoolExecutor 已提供的 4 种拒绝策略：
            CallerRunsPolicy：提交任务的线程自己去执行该任务。
            AbortPolicy：默认的拒绝策略，会 throws RejectedExecutionException。
            DiscardPolicy：直接丢弃任务，没有任何异常抛出。
            DiscardOldestPolicy：丢弃最老的任务，其实就是把最早进入工作队列的任务丢弃，然后把新任务加入到工作队列。
        */
    }
}

/**
 * 模拟线程池工作原理
 */
class MyThreadPool{
    // 阻塞队列-存储线程池消费者需要消费的任务
    private BlockingQueue<Runnable> workQueue;

    // 线程池线程个数
    private int poolSize;

    // 线程池内部的工作线程
    private List<Thread> threads = new ArrayList<>();

    // 构造函数
    public MyThreadPool(int poolSize, BlockingQueue<Runnable> workQueue) {
        this.poolSize = poolSize;
        this.workQueue = workQueue;
        for (int idx = 0; idx < poolSize; idx++){
            Thread thread = new WorkerThread();
            thread.start();
            threads.add(thread);
        }
    }

    // 生产者方法，不断的向队列中投入任务
    public void execute(Runnable task){
        try {
            workQueue.put(task);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 消费者方法，不断的消费队列中的任务执行
    class WorkerThread extends Thread{
        @Override
        public void run() {
            // 循环取任务并执行
            while (true){
                Runnable task = null;
                try {
                    task = workQueue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                task.run();
            }
        }
    }
}

