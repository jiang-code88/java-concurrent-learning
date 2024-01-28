package com.learn._02_synchronized;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * Java 中的信号量机制
 *  - 信号量模型：一个计数器，一个等待队列，三个方法 init()、down() 和 up()
 *  1）init()：设置计数器的初始值。
 *  2）down()：计数器的值减 1；如果此时计数器的值小于 0，则当前线程将被阻塞，否则当前线程可以继续执行。
 *  3）up()：计数器的值加 1；如果此时计数器的值小于或者等于 0，则唤醒等待队列中的一个线程，并将其从等待队列中移除。
 *
 *  - 实现一个互斥锁，仅仅是 Semaphore 的部分功能，Semaphore 还有一个功能是 Lock 不容易实现的，
 *    那就是：Semaphore 可以允许多个线程访问一个临界区。
 */
public class _13_Semaphore {

    private static long count = 0;
    // 使用信号量实现互斥锁功能-累加器
    private static final Semaphore s = new Semaphore(1);

    public static void add10K(){
        int idx = 0;
        while (idx++ < 10000){
            try {
                s.acquire();
                count++;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                s.release();
            }
        }
    }

    public static void count() throws InterruptedException {
        while (true){
            Thread threadA = new Thread(_13_Semaphore::add10K);
            Thread threadB = new Thread(_13_Semaphore::add10K);
            threadA.start();
            threadB.start();
            threadA.join();
            threadB.join();
            System.out.println("result: [" + count + "]");
            if (count != 20000){
                break;
            }
            count = 0;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 使用信号量实现互斥锁功能-累加器
        count();
    }
}

//

/**
 * 信号量实现-对象池的限流器
 *  - 对象池一次性创建 N 个对象，之后的所有线程重复利用这些 N 个对象。
 *  - 限流的意义是不允许多于 N 个线程同时进入临界区。
 *  - 如果不使用信号量机制实现限流，那就会出现多于 N 个的线程进入对象池导致取不出对象的异常。
 *
 * @param <T> 对象池的对象类型
 * @param <R>
 */
class ObjPool<T, R>{

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjPool.class);

    private final List<T> pool;

    private final Semaphore semaphore;

    public ObjPool(List<T> objs) {
        this.pool = new Vector<T>();
        this.pool.addAll(objs);
        this.semaphore = new Semaphore(objs.size());
    }

    public R exec(Function<T, R> func) throws InterruptedException {
        T t = null;
        semaphore.acquire(); // 信号量--
        try {
            t = pool.remove(0);
            return func.apply(t);
        }finally {
            // 有一个小 bug：如果在池中取不出对象抛出异常，
            // finally 还是会执行然后把初始的 null 放回队列。其实应该不是 null 才放回池
            pool.add(t);
            semaphore.release(); // 信号量++
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int size = 5;
        int threads = 10;

        ArrayList<String> objs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            objs.add("obj_" + i);
        }
        ObjPool<String, String> objectPool = new ObjPool<>(objs);

        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(()->{
                try {
                    objectPool.exec(obj -> {
                        LOGGER.info("当前线程 id=[{}] name=[{}]，获取的对象 obj=[{}]",
                                Thread.currentThread().getId(),
                                Thread.currentThread().getName(), obj);
                        return obj;
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
        }
    }

}





