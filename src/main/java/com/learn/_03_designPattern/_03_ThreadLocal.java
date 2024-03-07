package com.learn._03_designPattern;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程本地存储模式
 *  - 没有共享就没有任务线程安全问题，也就是使得每个线程都拥有自己的变量，
 *    彼此之间不共享，也就没有并发问题了。
 *  - Java 语言提供的线程本地存储（ThreadLocal）能够实现每个线程独享的变量，
 *    避免共享也就能避免线程安全问题。
 *  - 手动启动的线程使用 ThreadLocal 变量不需要担心内存泄漏问题，而线程池使用 ThreadLocal 变量存在内存泄漏问题，
 *     因此使用完后建议及时手动释放。
 *
 *  常用场景：
 *   1）如果你需要在并发场景中使用一个线程不安全的工具类，最简单的方案就是避免共享。
 *      避免共享有两种方案，一种方案是将这个工具类作为局部变量使用，另外一种方案就是线程本地存储模式。
 *      这两种方案，局部变量方案的缺点是在高并发场景下会频繁创建对象，
 *      而线程本地存储方案，每个线程只需要创建一个工具类的实例，所以不存在频繁创建对象的问题。
 *   2）在线程执行开始前设置 ThreadLocal 变量，执行完删除，
 *      这种用法一般是为了在一个线程里传递全局参数，也叫上下文信息，
 *      因为局部变量不能跨方法，适用于在中间业务代码中调用多个方法，作为多个方法共享变量的场景。比如 user 信息。
 *
 *  ThreadLocal 变量不支持继承共享
 *   - 通过 ThreadLocal 创建的线程变量，其子线程是无法继承的。
 *     也就是说你在线程中通过 ThreadLocal 创建了线程变量 V，
 *     而后该线程创建了子线程，你在子线程中是无法通过 ThreadLocal 来访问父线程的线程变量 V 的。
 */
public class _03_ThreadLocal {

    private static final AtomicLong nextId = new AtomicLong(0);

    // 定义 ThreadLocal 变量
    private static final ThreadLocal<Long> threadLocalId = ThreadLocal.withInitial(()->{
            return nextId.getAndIncrement();
    });

    // 线程获取 ThreadLocal 变量
    private static long get(){
        return threadLocalId.get();
    }

    public static void main(String[] args) {
        // 1 手动启动线程使用 ThreadLocal 场景
        // 随着 Thread 对象的回收，线程拥有的 ThreadLocal 变量也回被自动回收掉，避免容易发生的内存泄漏
        // TheadLocalTest();

        // 2 线程池中线程使用 ThreadLocal 场景
        // 线程池中，线程的 Thread 对象会持续存在，所以建议手动释放线程拥有的 ThreadLocal 变量，避免潜在的内存泄漏风险
        ThreadPoolTheadLocalTest();
    }

    public static void TheadLocalTest(){
        // 同一个线程多次调用，返回的 id 值相同
        new Thread(()->{
            long id1 = get();
            long id2 = get();
            System.out.println(Thread.currentThread().getName() + ": "
                    + "id1: " + id1 + " "
                    + "id2: " + id2);
        }).start();

        // 不同线程多次调用，返回的 id 值不同
        new Thread(()->{
            long id = get();
            System.out.println(Thread.currentThread().getName() + ": " + "id: " + id);
        }).start();
        new Thread(()->{
            long id = get();
            System.out.println(Thread.currentThread().getName() + ": " + "id: " + id);
        }).start();
    }

    public static void ThreadPoolTheadLocalTest(){
        ExecutorService executor = Executors.newFixedThreadPool(5);
        int threadNum = 5;
        for (int i = 0; i < threadNum; i++) {
            executor.execute(() -> {
                // 线程设置一个 ThreadLocal 变量
                threadLocalId.set(100L);
                try {
                    // 执行可能出现异常的业务逻辑代码
                    System.out.println(Thread.currentThread().getName() + " : " + threadLocalId.get());
                    // 因为局部变量不能跨方法，适用于在中间业务代码中调用多个方法，ThreadLocal 变量作为同一线程内多个方法共享变量的场景。
                } finally {
                    // 手动清理 Thread 拥有的 ThreadLocal 变量
                    threadLocalId.remove();
                }
            });
        }
        executor.shutdown();
    }
}

/**
 * SimpleDateFormat 不是线程安全的，在并发场景下通过 ThreadLocal 线程安全的使用它
 */
class SafeDateFormat{

    // 多线程共享同一个 SimpleDateFormat 对象时会出现线程不安全问题
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 定义 ThreadLocal 变量
    private static final ThreadLocal<SimpleDateFormat> tl = ThreadLocal.withInitial(()->{
       return new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
    });

    // 不同线程执行 get() 方法，返回的 SimpleDateFormat 对象是不同的，
    // 相当于不同线程通过调用 get() 方法获取一个线程专属的 SimpleDateFormat 对象局部变量，
    // 避免共享使用同一个 SimpleDateFormat 对象，防止线程不安全问题出现。
    public static SimpleDateFormat get(){
        return tl.get();
    }

    public static void main(String[] args) {
        int threadNum = 2;
        for (int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(()->{
                // SimpleDateFormat 对象对 hashcode 方法进行重写，所以不同对象的 toString 方法输出是一致的
                // 因此不能通过 toString 判断对象是否相等

                SimpleDateFormat simpleDateFormat = get();
                SimpleDateFormat simpleDateFormatRepeat = get();

                // 打印对象的内存地址，可以输出不同线程获取到不同的 SimpleDateFormat 对象
                int TheadLocalHashCode = System.identityHashCode(simpleDateFormat);
                // 同一个线程重复通过 ThreadLocal 获取 SimpleDateFormat 都是同一个对象
                int TheadLocalHashCodeRepeat = System.identityHashCode(simpleDateFormatRepeat);

                // 如果是线程共享变量，不同线程获取的都是同一个 SimpleDateFormat 对象
                int staticHashcode = System.identityHashCode(format);

                System.out.println(Thread.currentThread().getName() + " : "
                        + TheadLocalHashCode + " : " + TheadLocalHashCodeRepeat + " : " + staticHashcode);
            });
            thread.start();
        }
    }

}