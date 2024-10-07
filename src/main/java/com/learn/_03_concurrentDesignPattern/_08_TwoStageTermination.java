package com.learn._03_concurrentDesignPattern;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 两阶段终止模式——优雅地终止正在运行的线程
 *  - 所谓优雅就是给被终止的线程一个料理后事的机会，而不是一剑封喉）
 *  1. 第一阶段：发送终止指令，使用 interrupt 方法把线程从休眠状态转化到 RUNNABLE 状态。
 *  2. 第二阶段：响应终止指令，线程执行时会去检查终止标志位，如果符合终止条件就自己结束线程运行。
 *
 * 线程池的优雅终止
 *  - 线程池提供了两个方法：shutdown() 和 shutdownNow() 用于终止线程池中线程的运行。
 *  - shutdown() 是保守的关闭线程池的方法，只会影响到阻塞队列接受任务。
 *  - shutdownNow() 是激进的关闭线程池的方法，会影响到线程池所有的任务。
 */
public class _08_TwoStageTermination {
    public static void main(String[] args) {
        ExecutorService es =
                Executors.newFixedThreadPool(2);
        // 线程池执行 shutdown() 后，会拒绝接收新的任务，
        // 但是会等待线程池中正在执行的任务和已经进入阻塞队列的任务都执行完之后才最终关闭线程池。
        es.shutdown();

        // 线程池执行 shutdownNow() 后，会拒绝接收新的任务，
        // 同时还会中断线程池中正在执行的任务，已经进入阻塞队列的任务也被剥夺了执行的机会，
        // 不过这些被剥夺执行机会的任务会作为 shutdownNow() 方法的返回值返回。
        es.shutdownNow();
    }
}

/**
 * 需求：
 *   - 监控系统需要动态地采集一些数据，「监控系统」发送采集指令给「被监控系统」的「监控代理」，
 *     「监控代理」接收到指令之后，从「监控目标」收集数据，然后回传给「监控系统」。
 *   - 出于对性能的考虑，动态采集功能一般都会有终止操作，也就是终止「监控代理」线程对「监控目标」的数据收集和回传。
 * 问题：
 *   - 使用 Thread.currentThread().isInterrupted() 作为线程是否被终止的标志位
 *     但是这存在缺陷，那就是如果在 catch 中捕获到中断异常，但是没有重置，将会导致
 *     线程不能被合理的终止而是持续的运行下去。
 */
class Proxy{
    private static boolean started = false;
    private static Thread rptThread;

    // 监控代理启动采集，回收功能
    public synchronized static void start(){
        // 不允许同时启动多个采集线程
        if (started){
            return;
        }
        started = true;

        rptThread = new Thread(() -> {
            // 线程是否被终止的标志位
            while (!Thread.currentThread().isInterrupted()) {
                // 省略采集，回传的具体实现
                // 每隔 2 s，采集回传一次数据
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // 重置线程的终端状态
                    Thread.currentThread().interrupt();
                }
            }
            started = false;
        });
        rptThread.start();
    }

    // 中止监控代理的采集，回收功能
    public synchronized static void stop(){
        rptThread.interrupt();
    }
}

/**
 * 优化「监控代理」的中止操作：
 *   - 尽可能的设置自己的线程终止标志位，避免使用 Thread.currentThread().isInterrupted()
 *     防止如果在调用第三方类库的情况下，第三方类库没有在捕获到异常时正确重置线程终止标志，导致的线程无法被正确终止。
 *
 *   - 在 catch Thread 的 sleep、wait 等方法抛出的异常时，建议都重置一遍以防外界调用方使用
 *     Thread.currentThread().isInterrupted() 作为线程终止标志时，没有正确重置发生问题。
 */
class ProxyCustomTerminalFlag{

    // terminated 变量的写在 synchronized 同步方法 stop 中，
    // 但是读是在启动的线程中单独读的，synchronized 同步方法 start 管不到，所以需要使用 volatile 关键字保证可见性。
    private static volatile boolean terminated = false;
    // started 变量的读写都在 synchronized 同步方法中，所以不需要使用 volatile 关键字保证可见性。
    // 
    private static boolean started = false;
    private static Thread rptThread;

    // 启动采集功能
    public synchronized static void start(){
        // 不允许同时启动多个采集线程
        if (started){
            return;
        }
        started = true;
        rptThread = new Thread(() -> {
            while (!terminated) {
                // 省略采集，回传的具体实现
                // 每隔 2 s，采集回传一次数据
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // 重置线程的终端状态
                    // 这里设置的理由是：尽可能的规范，如果调用方使用 Thread.currentThread().isInterrupted()
                    // 作为线程终止的标志，这里 catch 有可能隐蔽的重置掉，干扰调用方的线程终止。
                    Thread.currentThread().interrupt();
                }
            }
            started = false;
        });
        rptThread.start();
    }

    // 终止采集功能
    public synchronized static void stop(){
        // 设置线程终止标志位
        terminated = true;
        // 中断线程 rptThread
        rptThread.interrupt();
    }
}


