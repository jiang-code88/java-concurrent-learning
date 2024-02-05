package com.learn._03_designPattern;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程本地存储模式
 *  - 没有共享就没有任务线程安全问题
 *  - Java 语言提供的线程本地存储（ThreadLocal）能够实现每个线程独享的变量，避免共享也就避免线程安全问题。
 */
public class _03_ThreadLocal {

    private static final AtomicLong nextId = new AtomicLong(0);

    // 定义 ThreadLocal 变量
    private static final ThreadLocal<Long> threadLocalId
            = ThreadLocal.withInitial(()->{
                return nextId.getAndIncrement();
    });

    public static void TheadLocalTest(){
        int threadNum = 5;
        for (int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(()->{
                // 为每一个线程分配一个唯一的变量
                Long id = threadLocalId.get();
                System.out.println(Thread.currentThread().getName() + " : " + id);
            });
            thread.start();
        }
    }

    public static void ThreadPoolTest(){
        ExecutorService executor = Executors.newFixedThreadPool(5);
        int threadNum = 5;
        for (int i = 0; i < threadNum; i++) {
            executor.execute(() -> {
                // 线程获取一个 ThreadLocal 变量
                Long id = threadLocalId.get();
                try {
                    // 执行可能出现异常的业务逻辑代码
                    System.out.println(Thread.currentThread().getName() + " : " + id);
                } finally {
                    // 手动清理 ThreadLocal 变量
                    threadLocalId.remove();
                }
            });
        }
        executor.shutdown();
    }

    public static void main(String[] args) {
        // 1 手动启动线程场景，使用 ThreadLocal
        // 随着 Thread 对象的回收，ThreadLocal 变量也回被自动回收掉，避免容易发生的内存泄漏
        // TheadLocalTest();

        // 2 线程池场景，如何正确使用 ThreadLocal
        // 线程池中，线程的 Thread 对象会持续存在，所以建议手动释放 ThreadLocal 变量
        ThreadPoolTest();
    }
}

class SafeDateFormat{

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 定义 ThreadLocal 变量
    private static final ThreadLocal<DateFormat> tl = ThreadLocal.withInitial(()->{
       return new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
    });

    // 不同线程执行下面代码，返回的 SimpleDateFormat 对象是不同的
    public static DateFormat get(){
        return tl.get();
    }

    public static void main(String[] args) {
        int threadNum = 5;
        for (int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(()->{
                // SimpleDateFormat 对象对 hashcode 方法进行重写，所以不同对象的 toString 输出是一致的
                DateFormat dateFormat = get();
                // 打印对象的内存地址，可以输出每个线程获取到不同的 SimpleDateFormat 对象
                int hashCode = System.identityHashCode(dateFormat);
                System.out.println(Thread.currentThread().getName() + " : "
                        + dateFormat + " : " + hashCode);

            });
            thread.start();
        }
    }

}
