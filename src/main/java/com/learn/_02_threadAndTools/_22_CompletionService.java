package com.learn._02_threadAndTools;

import com.learn.common.CommTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 批量提交异步任务时建议使用 CompletionService：
 *  - CompletionService 将线程池 Executor 和阻塞队列 BlockingQueue 的功能融合在了一起，
 *    能够让批量异步任务的管理更简单。
 *  - 除此之外，CompletionService 能够让异步任务的执行结果有序化，
 *    先执行完的先进入阻塞队列，利用这个特性，你可以轻松实现后续处理的有序性，避免无谓的等待，
 *
 *  - CompletionService 实现原理是内部维护了一个阻塞队列，
 *    当任务执行结束就把任务执行结果的 Future 对象加入到阻塞队列中。
 *
 *  - CompletionService 接口的实现类是 ExecutorCompletionService
 *    构造时都需要传入一个线程池和可选传入阻塞队列 completionQueue，
 *    如果不指定阻塞队列则默认使用无界的 LinkedBlockingQueue。
 *
 *  - take()、poll() 都是从阻塞队列中获取并移除一个元素；
 *    它们的区别在于如果阻塞队列是空的，那么调用 take() 方法的线程会被阻塞，而 poll() 方法会返回 null 值。
 *  - poll(long timeout, TimeUnit unit) 方法支持以超时的方式获取并移除阻塞队列头部的一个元素，
 *    如果等待了 timeout unit 时间，阻塞队列还是空的，那么该方法会返回 null 值。
 */
public class _22_CompletionService {
    private static List<Integer> myDB = new Vector<>();

    // 向电商一询价
    public static int getPriceByS1(){
        CommTools.sleep(300, TimeUnit.MILLISECONDS);
        return 100;
    }
    // 向电商二询价
    public static int getPriceByS2(){
        CommTools.sleep(200, TimeUnit.MILLISECONDS);
        return 200;
    }
    // 向电商三询价
    public static int getPriceByS3(){
        CommTools.sleep(100, TimeUnit.MILLISECONDS);
        return 300;
    }

    // 保存价格到自家数据库
    public static void save(int price){
        CommTools.sleep(100, TimeUnit.MILLISECONDS);
        myDB.add(price);
    }

    // 1 串行操作，性能很慢
    public static void inquirySerial(){
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

    // 2 使用「线程池 + Future」优化，性能有所提高
    // 但是存在的问题：如果 f3 先于 f1 执行完成，也得等到 f1.get() 执行完成才能被保存。
    public static void inquiryParallel()
            throws ExecutionException, InterruptedException {
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
        // 存在的问题：如果 f3 先于 f1 执行完成，也得等到 f1.get() 执行完成才能被保存。

        latch.await();
        executor.shutdown();
    }

    // 3 使用「线程池 + Future + 阻塞队列」优化，解决存在的问题
    public static void inquiryParallelBlocking() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        // 创建阻塞队列
        LinkedBlockingQueue<Integer> results = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(3);

        // 异步的将各电商的询价结果加入阻塞队列中
        Future<Integer> f1 = executorService.submit(() ->{
            int price = getPriceByS1();
            try {
                // 询价操作结束就将结果放入阻塞队列
                results.put(price);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return price;
        });
        Future<Integer> f2 = executorService.submit(() ->{
            int price = getPriceByS2();
            try {
                // 询价操作结束就将结果放入阻塞队列
                results.put(price);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return price;
        });
        Future<Integer> f3 = executorService.submit(() -> {
            int price = getPriceByS3();
            try {
                // 询价操作结束就将结果放入阻塞队列
                results.put(price);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return price;
        });

        // 异步的保存各电商的询价结果
        // 那个任务先完成就把结果先放入阻塞队列，所以可以解决问题
        for (int i = 0; i < 3; i++) {
            Integer result = results.take();
            executorService.execute(()->{
                save(result);
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();
    }

    // 4 使用 completionService 优化，使得主线程阻塞，然后谁先完成就谁先存。
    public static void inquiryCompletionService()
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        // CompletionService 默认会使用无界的 LinkedBlockingQueue 作为阻塞队列
        ExecutorCompletionService<Integer> completionService =
                new ExecutorCompletionService<>(executor);

        completionService.submit(() -> getPriceByS1());
        completionService.submit(() -> getPriceByS2());
        completionService.submit(() -> getPriceByS3());

        // 最低报价
        AtomicReference<Integer> minValue =
                new AtomicReference<>(Integer.MAX_VALUE);

        for (int i = 0; i < 3; i++) {
            // S1、S2、S3 谁先完成就谁先存
            executor.execute(()->{
                Integer price;
                try {
                    // 如果阻塞队列是空的，take() 方法会阻塞，
                    // get() 方法用于获取 Future 对象中的执行结果
                    price = completionService.take().get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                save(price);
                // 计算最低报价
                minValue.getAndUpdate((v)->Integer.min(v, price));
                latch.countDown();
            });
        }

        latch.await();
        System.out.println("minValue: " + minValue);
        executor.shutdown();
    }

    /* 实现询价应用 */
    public static void main(String[] args) {
        // 920 毫秒
        // 1 串行操作，性能很慢
        CommTools.countTimeExec(()->{
            inquirySerial();
        });

        // 410 毫秒
        // 2 使用「线程池 + Future」优化，性能有所提高
        // CommTools.countTimeExec(()->{
        //     try {
        //         inquiryParallel();
        //     } catch (ExecutionException | InterruptedException e) {
        //         throw new RuntimeException(e);
        //     }
        // });

        // 3 使用「线程池 + Future + 阻塞队列」优化，解决存在的问题
        // 模拟类似 CompletionService 的原理
        // CommTools.countTimeExec(()->{
        //     try {
        //         inquiryParallelBlocking();
        //     } catch (InterruptedException e) {
        //         throw new RuntimeException(e);
        //     }
        // });

        // 410 毫秒
        // 4 使用 completionService 优化，使得主线程阻塞，然后谁先完成就谁先存。
        // CommTools.countTimeExec(()->{
        //     try {
        //         inquiryCompletionService();
        //     } catch (InterruptedException e) {
        //         throw new RuntimeException(e);
        //     }
        // });

        // 打印询价应用保存的价格结果
        for (Integer price : myDB) {
            System.out.println(price);
        }
    }
}


/**
 * 利用 CompletionService 实现 Dubbo 中的 Forking Cluster 功能，
 * 也就是并行地调用多个查询服务，只要有一个成功返回结果，整个服务就可以返回了
 *
 * 场景：
 *  为了服务的高可用和性能，地址转坐标服务会并行地调用 3 个地图服务商的 API，
 *  然后只要有 1 个正确返回了结果 r，那么地址转坐标这个服务就可以直接返回 r 了。
 *  这种集群模式可以容忍 2 个地图服务商服务异常，但缺点是消耗的资源偏多。
 */
class ForkingCluster{

    private static Integer getCodeByS1(String addr){
        CommTools.sleep(100, TimeUnit.MILLISECONDS);
        // 模拟该服务商户不可用
        return null;
    }
    private static Integer getCodeByS2(String addr){
        CommTools.sleep(100, TimeUnit.MILLISECONDS);
        return 100;
    }
    private static Integer getCodeByS3(String addr){
        CommTools.sleep(100, TimeUnit.MILLISECONDS);
        // 模拟该服务商户不可用
        return null;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        ExecutorCompletionService<Integer> completionService
                = new ExecutorCompletionService<>(executor);
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

        // 打印结果
        System.out.println(result);
    }
}
