package com.learn._03_designPattern;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 两阶段终止模式，优化的终止正在运行的线程
 * 1. 第一阶段：发送终止指令，使用 interrupt 方法把线程从休眠状态转化到 RUNNABLE 状态
 * 2. 第二阶段：响应终止指令，线程执行时检查终止标志位，如果符合终止条件则自动结束线程运行
 */
public class _08_TwoStageTermination {
    public static void main(String[] args) {
        ExecutorService es =
                Executors.newFixedThreadPool(2);
        // 线程池执行 shutdown() 后，就会拒绝接收新的任务，
        // 但是会等待线程池中正在执行的任务和已经进入阻塞队列的任务都执行完之后才最终关闭线程池。
        es.shutdown();

        // 线程池执行 shutdownNow() 后，会拒绝接收新的任务，同时还会中断线程池中正在执行的任务，
        // 已经进入阻塞队列的任务也被剥夺了执行的机会，
        // 不过这些被剥夺执行机会的任务会作为 shutdownNow() 方法的返回值返回。
        es.shutdownNow();

    }
}

/**
 * 使用 Thread.currentThread().isInterrupted() 作为线程是否被终止的标志位
 * 但是这存在一次缺陷，如果在 catch 中捕获中断异常，但是没有重置，将会导致
 * 线程不能被合理的终止而是持续的运行下去。
 */
class Proxy{

    private static boolean started;

    private static Thread rptThread;

    // 启动采集功能
    public synchronized static void start(){
        // 不允许同时启动多个采集线程
        if (started){
            return;
        }
        started = true;
        rptThread = new Thread(() -> {
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

    // 终止采集功能
    public synchronized static void stop(){
        rptThread.interrupt();
    }
}

/**
 * 尽可能的设置自己的线程终止标志位，避免使用 Thread.currentThread().isInterrupted()
 * 防止如果调用第三方类库的情况下，第三方类库没有正确重置线程终止标志，导致的线程无法被终止。
 *
 * 同时在 catch Thread 的 sleep、wait 等方式时，建议都重置一遍以防外界调用方使用
 * Thread.currentThread().isInterrupted() 作为线程终止标志时，没有正确重置发生问题。
 */
class ProxyCustomTerminalFlag{

    private static volatile boolean terminated = false;

    private static volatile boolean started = false;

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


