package com.learn._02_threadAndTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 批量提交异步任务时建议使用 CompletionService：
 * CompletionService 将线程池 Executor 和阻塞队列 BlockingQueue 的功能融合在了一起，
 * 能够让批量异步任务的管理更简单。除此之外，CompletionService 能够让异步任务的执行结果有序化，
 * 先执行完的先进入阻塞队列，利用这个特性，你可以轻松实现后续处理的有序性，避免无谓的等待，
 *
 * 其实现原理是内部维护了一个阻塞队列，当任务执行结束就把任务的执行结果加入到阻塞队列中
 * 不同的是 CompletionService 是把任务执行结果的 Future 对象加入到阻塞队列中。
 *
 * CompletionService 的方法
 * 如果阻塞队列是空的，那么调用 take() 方法的线程会被阻塞，而 poll() 方法会返回 null 值
 * poll(long timeout, TimeUnit unit) 方法支持以超时的方式获取并移除阻塞队列头部的一个元素，
 * 如果等待了 timeout unit 时间，阻塞队列还是空的，那么该方法会返回 null 值。
 */
public class _22_CompletionService {
    private static List<Integer> myDB = new Vector<>();

    public static int getPriceByS1(){
        sleep(300, TimeUnit.MILLISECONDS);
        return 100;
    }
    public static int getPriceByS2(){
        sleep(200, TimeUnit.MILLISECONDS);
        return 200;
    }
    public static int getPriceByS3(){
        sleep(100, TimeUnit.MILLISECONDS);
        return 300;
    }
    public static void save(int price){
        sleep(100, TimeUnit.MILLISECONDS);
        myDB.add(price);
    }

    public static void sleep(long t, TimeUnit u){
        try {
            u.sleep(t);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 串行操作，性能很慢
    public static void inquiry_version1(){
        // 向电商S1询价，并保存
        int r1 = getPriceByS1();
        save(r1);
        // 向电商S2询价，并保存
        int r2 = getPriceByS2();
        save(r2);
        // 向电商S3询价，并保存
        int r3 = getPriceByS3();
        save(r3);
    }

    // 使用 Future + 线程池 优化，性能有所提高
    public static void inquiry_version2() throws ExecutionException, InterruptedException {
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        // 异步向电商询价
        Future<Integer> f1 = executor.submit(() -> getPriceByS1());
        Future<Integer> f2 = executor.submit(() -> getPriceByS2());
        Future<Integer> f3 = executor.submit(() -> getPriceByS3());

        // 获取电商报价并异步保存
        Integer price1 = f1.get();
        executor.execute(()->{
            save(price1);
            latch.countDown();
        });
        Integer price2 = f2.get();
        executor.execute(()->{
            save(price2);
            latch.countDown();
        });
        Integer price3 = f3.get();
        executor.execute(()->{
            save(price3);
            latch.countDown();
        });

        latch.await();
        executor.shutdown();
    }

    // 使用 completionService 优化，使得主线程阻塞，然后谁先完成就谁先存。
    public static void inquiry_version3() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        // 默认会使用无界的 LinkedBlockingQueue 作为阻塞队列
        ExecutorCompletionService<Integer> completionService =
                new ExecutorCompletionService<Integer>(executor);

        completionService.submit(() -> getPriceByS1());
        completionService.submit(() -> getPriceByS2());
        completionService.submit(() -> getPriceByS3());

        AtomicReference<Integer> minValue =
                new AtomicReference<>(Integer.MAX_VALUE);

        for (int i = 0; i < 3; i++) {
            // S1、S2、S3 谁先完成就谁先存
            executor.execute(()->{
                Integer price;
                try {
                    price = completionService.take().get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                save(price);
                minValue.getAndUpdate((v)->Integer.min(v, price));
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("minValue: " + minValue);
    }

    public static void main(String[] args) {
        // 920 毫秒
        // countTimeExec(o->{
        //     inquiry_version1();
        //     return null;
        // });

        // 410 毫秒
        // countTimeExec(o->{
        //     try {
        //         inquiry_version2();
        //     } catch (ExecutionException e) {
        //         throw new RuntimeException(e);
        //     } catch (InterruptedException e) {
        //         throw new RuntimeException(e);
        //     }
        //     return null;
        // });

        // 410 毫秒
        countTimeExec(o->{
            try {
                inquiry_version3();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });


        for (Integer price : myDB) {
            System.out.println(price);
        }
    }

    public static void countTimeExec(Function func){
        long startTime = System.currentTimeMillis();
        func.apply(null);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("程序执行时间：" + executionTime + " ms");
    }
}

/**
 * 利用 CompletionService 实现 Dubbo 中的 Forking Cluster
 * 地址转坐标服务
 */
class ForkingCluster{

    private static Integer getCodeByS1(String addr){
        _22_CompletionService.sleep(100, TimeUnit.MILLISECONDS);
        return null;
    }
    private static Integer getCodeByS2(String addr){
        _22_CompletionService.sleep(100, TimeUnit.MILLISECONDS);
        return 100;
    }
    private static Integer getCodeByS3(String addr){
        _22_CompletionService.sleep(100, TimeUnit.MILLISECONDS);
        return null;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        ExecutorCompletionService<Integer> completionService
                = new ExecutorCompletionService<Integer>(executor);
        ArrayList<Future<Integer>> futures = new ArrayList<>();

        String addr = "address";
        futures.add(completionService.submit(()->getCodeByS1(addr)));
        futures.add(completionService.submit(()->getCodeByS2(addr)));
        futures.add(completionService.submit(()->getCodeByS3(addr)));
        // 获取最快返回的任务结果
        Integer result = null;
        try {
            for (int i = 0; i < 3; i++) {
                result = completionService.take().get();
                if (result != null){
                    break;
                }
            }
        }finally {
            // 取消所有任务
            for (Future<Integer> future : futures) {
                future.cancel(true);
            }
            executor.shutdown();
        }

        System.out.println(result);
    }
}
