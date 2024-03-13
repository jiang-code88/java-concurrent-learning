package com.learn._01_concurrentFundament;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java 线程的线程状态
 *
 * Java 线程状态转换关系图
 * NEW
 * ｜
 * RUNNABLE <-> BLOCKED / WAITING / TIME_WAITING
 * ｜
 * TERMINATED
 *
 * 1. 如果 Java 线程处于 blocked / waiting / time_waiting 状态之一，那么这个线程就永远没有 CPU 的使用权。
 *
 * 2. RUNNABLE 与 BLOCKED 的状态转换：
 *    - 线程等待 synchronized 的隐式锁时，等待的线程就会从 RUNNABLE 转换到 BLOCKED 状态。
 *      而当等待的线程获得 synchronized 隐式锁时，就又会从 BLOCKED 转换到 RUNNABLE 状态。
 *
 * 3. RUNNABLE 与 WAITING 的状态转换：
 *    - 获得 synchronized 隐式锁的线程，调用无参数的 Object.wait() 方法。
 *    - 调用无参数的 Thread.join() 方法，例如有一个线程对象 thread A，
 *      当调用 A.join() 的时候，执行这条语句的线程会等待 thread A 执行完，
 *      而等待中的这个线程，其状态会从 RUNNABLE 转换到 WAITING。
 *      当线程 thread A 执行完，原来等待它的线程又会从 WAITING 状态转换到 RUNNABLE。
 *    - 调用 LockSupport.park() 方法。调用 LockSupport.park() 方法，
 *      当前线程会阻塞，线程的状态会从 RUNNABLE 转换到 WAITING。
 *      调用 LockSupport.unpark(Thread thread) 可唤醒目标线程，
 *      目标线程的状态又会从 WAITING 状态转换到 RUNNABLE。
 *
 * 4. RUNNABLE 与 TIMED_WAITING 的状态转换：
 *    1）调用带超时参数的 Thread.sleep(long millis) 方法；
 *    2）获得 synchronized 隐式锁的线程，调用带超时参数的 Object.wait(long timeout) 方法；
 *    3）调用带超时参数的 Thread.join(long millis) 方法；
 *    4）调用带超时参数的 LockSupport.parkNanos(Object blocker, long deadline) 方法；
 *    5）调用带超时参数的 LockSupport.parkUntil(long deadline) 方法。
 *
 * 5. NEW 到 RUNNABLE 状态
 *    <code>
 *     MyThread myThread = new MyThread();
 *     // 从NEW状态转换到RUNNABLE状态
 *     myThread.start()；
 *    </code>
 *
 * 6. RUNNABLE 到 TERMINATED 状态
 *    - 线程执行完 run() 方法后，会自动转换到 TERMINATED 状态，
 *      当然如果执行 run() 方法的时候异常抛出，也会导致线程终止。
 *    - 强制中断 run() 方法：调用 interrupt() 方法，仅仅是通知线程，
 *      线程有机会执行一些后续操作，同时也可以无视这个通知。
 *
 *    当线程 A 处于 WAITING、TIMED_WAITING 状态时，
 *    如果其他线程调用线程 A 的 interrupt() 方法，会使线程 A 返回到 RUNNABLE 状态，
 *    同时线程 A 的代码会触发 InterruptedException 异常。
 *
 *    如果线程处于 RUNNABLE 状态，并且没有阻塞在某个 I/O 操作上，
 *    这时就得依赖线程 A 主动检测中断状态了。
 *    如果其他线程调用线程 A 的 interrupt() 方法，
 *    那么线程 A 可以通过 isInterrupted() 方法，检测是不是自己被中断了。
 */

/**
 * 07 局部变量不存在线程安全问题：
 *
 * 1.每个线程有自己独立的调用栈，不同线程的调用栈互相干扰。
 * 2.线程调用每个方法就生成一个栈帧，压入函数调用栈中。
 * 3.每个方法调用的参数和局部变量都保存在栈帧中。
 * 4.局部变量是和方法同生共死的，一个变量如果想跨越方法（线程）的边界，就必须创建在堆里。
 *
 * 5.由于方法中的局部变量，不存在共享，所以即使不同步也不会有并发问题，这个技术叫做 「线程封闭」
 *   官方解释是：仅在单线程内访问数据。
 * 6.对象在堆里，对象的引用（句柄/指针）在栈里。
 * 7.Java 是引用传递，new 一个 List 对象，通过参数传递给方法。方法操作的是同一个 List 对象。
 */

public class _08_ThreadStatus {

    private static Logger LOGGER = LoggerFactory.getLogger(_08_ThreadStatus.class);

    public static void main(String[] args) throws InterruptedException {
        // 1.中止正在运行的线程
        // interruptThread();

        // 2.中止处于 TIMED_WAITING 状态的线程
        //   执行 sleep 函数处于 TIMED_WAITING 状态的线程是无法被正常中止的
        // interruptSleepThread();

        // 3.正确中止处于 TIMED_WAITING 状态的线程
        // correctInterruptSleepThread();
    }

    public static void interruptThread() throws InterruptedException {
        Thread thread = new Thread(()->{
            Thread th = Thread.currentThread();
            while (true){
                if (th.isInterrupted()){
                    break;
                }
                LOGGER.info("子线程状态：{}", th.getState());
            }
            LOGGER.warn("子线程被中止执行");
        });
        thread.start();
        LOGGER.info("主线程启动子线程");

        Thread.sleep(300);

        thread.interrupt();
        LOGGER.info("主线程中止子线程");
    }

    public static void interruptSleepThread() throws InterruptedException {
        Thread thread = new Thread(()->{
            Thread th = Thread.currentThread();
            while (true){
                if (th.isInterrupted()){
                    break;
                }
                LOGGER.info("子线程状态：{}", th.getState());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    LOGGER.warn("子线程被中止执行", e);
                }
            }
        });
        thread.start();
        LOGGER.info("主线程启动子线程");

        Thread.sleep(300);

        thread.interrupt();
        LOGGER.info("主线程中止子线程");
    }

    public static void correctInterruptSleepThread() throws InterruptedException {
        Thread thread = new Thread(()->{
            Thread th = Thread.currentThread();
            while (true){
                if (th.isInterrupted()){
                    break;
                }
                LOGGER.info("子线程状态：{}", th.getState());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // 抛出 InterruptedException 异常并被 catch 捕获后，线程的中止标志会被重置回 RUNNABLE
                    // 所以捕获到异常后，需要设置回中止标志，让线程会停止
                    Thread.currentThread().interrupt();
                    LOGGER.warn("子线程被中止执行", e);
                }
            }
        });
        thread.start();
        LOGGER.info("主线程启动子线程");

        Thread.sleep(300);

        thread.interrupt();
        LOGGER.info("主线程中止子线程");
    }
}