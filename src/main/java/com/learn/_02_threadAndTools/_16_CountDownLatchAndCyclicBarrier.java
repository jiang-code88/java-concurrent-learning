package com.learn._02_threadAndTools;

import com.learn.common.CommTools;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * CountDownLatch 和 CyclicBarrier 是 Java 并发包提供的两个非常易用的线程同步工具类，
 * - CountDownLatch 主要用来解决一个线程等待多个线程的场景。
 * - CyclicBarrier 是一组线程之间互相等待。
 *   CyclicBarrier 的计数器是可以循环利用的，而且具备自动重置的功能，一旦计数器减到 0 会自动重置到你设置的初始值。
 *   除此之外，CyclicBarrier 还可以设置回调函数，可以说是功能丰富。
 *
 * 场景：
 * 用户 -->  「在线商城」  --> 订单               --> 订单库
 *          「物流派送」  --> 派送单              --> 派送单库
 *          「对账系统」  -->「未对账订单｜派送单」  --> 差异库
 *
 */
public class _16_CountDownLatchAndCyclicBarrier {
    // 订单库
    private static List<String> orderBank = new Vector<>();

    // 派送单库
    private static List<String> deliveryBank = new Vector<>();

    // 差异库
    private static List<String> diffBank = new Vector<>();

    // 查询未对账订单
    public static String getPOrders(){
        CommTools.sleep(1, TimeUnit.MILLISECONDS);
        return orderBank.remove(0);
    }

    // 查询派送单
    public static String getDOrders(){
        CommTools.sleep(1, TimeUnit.MILLISECONDS);
        return deliveryBank.remove(0);
    }

    // 执行对账操作
    public static String check(String pos, String dos){
        if (pos == null || dos == null){
            throw new RuntimeException("null bank recorde");
        }
        if (pos.equals(dos)){
            return "YES";
        }else {
            return "NO";
        }
    }

    // 将差异写入差异库
    public static void save(String diff){
        diffBank.add(diff);
    }

    // 对账系统
    public static void checkSystem(){
        while(!orderBank.isEmpty()){ // while(存在未对账订单)
            // 查询未对账订单
            String pos = getPOrders();
            // 查询派送单
            String dos = getDOrders();
            // 执行对账操作
            String diff = check(pos, dos);
            // 差异写入差异库
            save(diff);
        }
    }

    private static String pos = null;
    private static String dos = null;

    // 对账系统-多线程并发优化
    public static void checkSystemThreadOptimize() throws InterruptedException {
        while(!orderBank.isEmpty()){ // while(存在未对账订单)
            // 查询未对账订单
            Thread threadPos = new Thread(() -> {
                pos = getPOrders();
            });
            threadPos.start();

            // 查询派送单
            Thread threadDos = new Thread(()->{
                dos = getDOrders();
            });
            threadDos.start();

            // 等待 T1、T2 结束
            threadPos.join();
            threadDos.join();

            // 执行对账操作
            String diff = check(pos, dos);

            // 差异写入差异库
            save(diff);

            // 清空记录
            pos = null;
            dos = null;
        }
    }

    // 对账系统-线程池并发优化-使用 CountDownLatch 实现同步
    public static void checkSystemThreadPoolOptimize() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        while(!orderBank.isEmpty()){ // while(存在未对账订单)
            CountDownLatch latch = new CountDownLatch(2);

            // 查询未对账订单
            executorService.execute(() -> {
                pos = getPOrders();
                latch.countDown();
            });

            // 查询派送单
            executorService.execute(()->{
                dos = getDOrders();
                latch.countDown();
            });

            // 等待 T1、T2 结束
            latch.await();

            // 执行对账操作
            String diff = check(pos, dos);

            // 差异写入差异库
            save(diff);

            // 清空记录
            pos = null;
            dos = null;
        }
        executorService.shutdown();
    }

    // 对账系统-CyclicBarrier优化-优化为更加高效的三路并行
    public static void checkSystemCyclicBarrierOptimize() throws InterruptedException {
        // 订单队列
        Vector<String> posList = new Vector<>();

        // 派送单队列
        Vector<String> dosList = new Vector<>();

        CountDownLatch latch = new CountDownLatch(1000);

        // 执行回调的线程池
        Executor executor = Executors.newFixedThreadPool(1);
        CyclicBarrier barrier = new CyclicBarrier(2, ()->{
            executor.execute(()->{
                String P = posList.remove(0);
                String D = dosList.remove(0);
                // 执行对账操作
                String diff = check(P, D);
                // 差异写入差异库
                save(diff);
                latch.countDown();
            });
        });

        /* // 执行回调的线程池
           // 小优化：如果从两个队列中去元素的操作从异步中取出转为同步就可以开多几个线程操作，而不需要限制为单线程操作
        Executor executorA = Executors.newFixedThreadPool(10);
        CyclicBarrier barrieA = new CyclicBarrier(2, ()->{
            String P = posList.remove(0);
            String D = dosList.remove(0);
            executorA.execute(()->{
                // 执行对账操作
                String diff = check(P, D);
                // 差异写入差异库
                save(diff);
            });
        }); */

        // 查询未对账订单
        Thread threadGetOrder = new Thread(()->{
            while (!orderBank.isEmpty()){
                posList.add(getPOrders());
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        threadGetOrder.start();

        // 查询派送单
        Thread threadGetDelivery = new Thread(()->{
            while (!orderBank.isEmpty()){
                dosList.add(getDOrders());
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        threadGetDelivery.start();

        threadGetOrder.join();
        threadGetDelivery.join();
        latch.await();
    }


    public static void initBank(){
        for (int i = 0; i < 1000; i++) {
            orderBank.add(String.valueOf(i));
            deliveryBank.add(String.valueOf(i));
        }
    }
    public static void countTimeExec(Function func){
        initBank();

        long startTime = System.currentTimeMillis();
        func.apply(null);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("程序执行时间：" + executionTime + " ms");

        System.out.println("对应订单和派送单的差异信息（" + diffBank.size() + "）：");
        for (String diff : diffBank) {
            if (diff.equals("NO")){
                throw new RuntimeException("diff recorde");
            }
            System.out.println(diff);
        }
    }

    public static void main(String[] args) {
        // 1 对账系统操作耗时：2510 ms 左右
        // countTimeExec(object -> {
        //     checkSystem();
        //     return null;
        // });

        // 2 多线程并发优化-对账系统操作耗时：1360 ms 左右
        // countTimeExec(object -> {
        //     try {
        //         checkSystemThreadOptimize();
        //     } catch (InterruptedException e) {
        //         throw new RuntimeException(e);
        //     }
        //     return null;
        // });

        // 3 线程池并发优化-对账系统操作耗时：1290 ms 左右
        countTimeExec(object -> {
            try {
                checkSystemThreadPoolOptimize();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        // 4 CyclicBarrier并发优化-对账系统操作耗时：1270 ms 左右
        // countTimeExec(object -> {
        //     try {
        //         checkSystemCyclicBarrierOptimize();
        //     } catch (InterruptedException e) {
        //         throw new RuntimeException(e);
        //     }
        //     return null;
        // });

        System.out.println("end");
    }

}




/**
 * 判断所有子线程是否都执行完成的计数器
 *  - 主线程判断计时器是否为 0，如果不为 0 则主线程阻塞，为 0 则主线程继续执行，表示所有子线程已执行完。
 *  - 主线程持有一个计数器，每执行完一个子线程计数器减一，如果计时器等于 0，唤醒阻塞的线程。
 */
class ThreadNumber{
    private long runningTheadNum = 0;
    private final Lock lock = new ReentrantLock();
    private final Condition notEnd = lock.newCondition();

    public ThreadNumber(long runningTheadNum) {
        this.runningTheadNum = runningTheadNum;
    }

    public void decrementOne(){
        lock.lock();
        try{
            runningTheadNum--;
            if (runningTheadNum == 0){
                notEnd.signalAll();
            }
        }finally {
            lock.unlock();
        }
    }

    public void waitThreads(){
        lock.lock();
        try {
            while (runningTheadNum != 0){
                notEnd.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

}

/**
 * 使用上述的计时器实现：
 * 主线程能够等待所有子线程执行完成后再执行
 */
class MultiLock {
    // 账户余额
    private volatile long balance;
    // 保护账户余额的锁
    private final Object balLock = new Object();

    public MultiLock(long balance) {
        this.balance = balance;
    }

    // 取款
    public void withdraw(long amt){
        synchronized (balLock) {
            this.balance -= amt;
        }
    }

    // 查看余额
    public long getBalance(){
        synchronized (balLock) {
            return this.balance;
        }
    }

    // 并发测试程序是否存在线程安全问题
    public static void main(String[] args) throws InterruptedException {
        long count = 100; // 账户总余额
        int unit = 10;        // 每个线程每次取款金额

        MultiLock account = new MultiLock(count);
        System.out.println("初始金额：" + account.getBalance());

        // 启动多个线程, 将账户的钱取空
        long times = count / unit;
        ThreadNumber threadNumber = new ThreadNumber(times);
        for (int i = 0; i < times; i++) {
            new Thread(() -> {
                account.withdraw(unit);
                System.out.println("线程 name=[" + Thread.currentThread().getName() + "] 执行完成");
                threadNumber.decrementOne();
            }).start();
        }
        System.out.println("start withdraw threads end");

        // 主线程停一下, 保证所有子线程线程运行完
        threadNumber.waitThreads();

        System.out.println("剩余金额：" + account.getBalance());
        System.out.println("-----------------");
    }

}
