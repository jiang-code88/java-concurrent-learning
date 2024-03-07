package com.learn._02_concurrentUtils;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * 获取异步任务的执行结果
 *  - 场景：创建线程池，调用 execute() 提交任务，线程池执行任务，
 *         但是没办法获取任务的执行结果（execute() 方法没有返回值）
 *
 *  - 向线程池中提交执行异步任务：
 *    1）提交 Runnable 任务 Future submit(Runnable task);
 *    2）提交 Callable 任务 Future submit(Callable task);
 *    3）提交 Runnable 任务及结果引用 Future submit(Runnable task, T result);
 *
 *  - 获取异步任务执行结果的操作：
 *    1）取消任务boolean cancel( boolean mayInterruptIfRunning);
 *    2）判断任务是否已取消 boolean isCancelled();
 *    3）判断任务是否已结束boolean isDone();
 *    4）获得任务执行结果 get();
 *    5）获得任务执行结果，支持超时 get(long timeout, TimeUnit unit);
 *      这两个 get() 方法都是阻塞式的，如果被调用的时候，任务还没有执行完，
 *      那么调用 get() 方法的线程会阻塞，直到任务执行完才会被唤醒。
 *
 *  - FutureTask 工具类：
 *    1) 利用 FutureTask 对象可以更容易获取子线程的执行结果。把任务和返回结果组合在一起，
 *       变成一个可以有返回结果的任务进行提交，使用更方便。
 *    1) 可以将 FutureTask 对象作为任务提交给 ThreadPoolExecutor 去执行，也可以直接被 Thread 执行；
 *
 *  - Future 适用场景：
 *    利用多线程可以快速将一些串行的任务并行化，从而提高性能；如果任务之间有依赖关系，
 *    比如当前任务依赖前一个任务的执行结果，这种问题基本上都可以用 Future 来解决。
 */
public class _10_Future {
    // 创建线程池
    private static ExecutorService executor =
            Executors.newFixedThreadPool(1);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /* 1 Future<?> submit(Runnable task);
             Runnable 接口的 run() 方法是没有返回值的，
             这个方法返回的 Future 仅可以用来断言任务是否已经结束。*/
        // submitRunnableTask();

        /* 2 <T> Future<T> submit(Callable<T> task);
             Callable 接口的 call() 方法是有返回值的，
             这个方法返回的 Future 对象可以通过调用其 get() 方法来获取任务的执行结果。*/
        // submitCallableTask();

        /* 3 <T> Future<T> submit(Runnable task, T result);
             假设这个方法返回的 Future 对象是 f，f.get() 的返回值就是传给 submit() 方法的参数 result。
             result 相当于主线程和子线程之间的桥梁，通过它主子线程可以共享数据。 */
        // submitRunnableResultTask();

        /* 4 使用 Callable 接口和 submit 方法实现主子线程共享数据。*/
        // submitCallableResult();

        /* 5 FutureTask 工具类 的使用示例 */
        // 使用线程池执行 FutureTask 任务
        // futureTaskExecutor();
        // 使用普通线程执行 FutureTask 任务
        // futureTaskThread();

        executor.shutdown();
    }

    public static void submitRunnableTask(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                int num = 1 + 2;
            }
        };
        Future<?> resultTask = executor.submit(task);

        while (!resultTask.isDone()){
            System.out.println(resultTask.isDone()); // false
        }
        System.out.println(resultTask.isDone());     // true
    }

    public static void submitCallableTask() throws ExecutionException, InterruptedException {

        Callable<Integer> taskCallable = new Callable() {
            @Override
            public Object call() throws Exception {
                return 1 + 2;
            }
        };
        Future<Integer> resultCallable = executor.submit(taskCallable);
        System.out.println(resultCallable.get());
    }

    static class Result{
        private int id;
        private String name;
    }

    public static void submitRunnableResultTask() throws ExecutionException, InterruptedException {
        Result resultObj = new Result();
        resultObj.id = 108;

        class Task implements Runnable{
            private Result result;
            public Task(Result result) {
                this.result = result;
            }
            @Override
            public void run() {
                System.out.println(result.id);
                this.result.name = "Nancy";
            }
        }

        // submit 方法需要参数传入 result 对象的用意：
        // Runnable 接口是没有返回值的，所以执行完 run() 方法任务后是不知道要返回什么的，需要 result 参数指定 future 的返回
        Future<Result> resultFuture = executor.submit(new Task(resultObj), resultObj);
        Result result = resultFuture.get();

        System.out.println(resultObj + " === " + result);
        System.out.println(resultObj.id + " === " + result.id);
        System.out.println(resultObj.name + " === " + result.name);
    }

    public static void submitCallableResult() throws ExecutionException, InterruptedException {
        Result resultObj = new Result();
        resultObj.id = 86;

        class Task implements Callable<Result>{
            private Result result;
            public Task(Result result) {
                this.result = result;
            }
            @Override
            public Result call() {
                System.out.println(result.id);
                result.name = "Tony";
                return result;
            }
        }

        Future<Result> resultFuture = executor.submit(new Task(resultObj));
        Result result = resultFuture.get();

        System.out.println(resultObj + " === " + result);
        System.out.println(resultObj.id + " === " + result.id);
        System.out.println(resultObj.name + " === " + result.name);
    }

    public static void futureTaskExecutor() throws ExecutionException, InterruptedException {
        // 创建 FutureTask 任务对象
        // 把任务和返回结果结合，变成一个可以有返回结果的任务进行提交
        FutureTask<Integer> futureTask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 1 + 2;
            }
        });
        // 提交线程池执行任务
        executor.submit(futureTask);
        // 通过 FutureTask 任务即可获取线程执行结果，不再依赖线程池本身提交任务再获取异步任务执行结果。
        Integer result = futureTask.get();
        System.out.println(result);
    }

    public static void futureTaskThread() throws ExecutionException, InterruptedException {
        // 创建 FutureTask 任务对象
        FutureTask<Integer> futureTask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 1 + 2;
            }
        });
        // 提交线程执行
        Thread thread = new Thread(futureTask);
        thread.start();
        // 获取计算结果
        Integer result = futureTask.get();
        System.out.println(result);
    }
}


/**
 * 实现「烧水泡茶」程序
 * 场景：
 *  洗水壶1m --> 烧热水15m --> 泡茶
 *              洗茶壶1m
 *              洗茶杯2m
 *              拿茶叶1m
 *
 * 并发编程总结为三个核心问题：分工、同步和互斥。
 * 编写并发程序，首先要做的就是分工，所谓分工指的是如何高效地拆解任务并分配给线程：
 *  T1 => 洗水壶 + 烧热水 + 泡茶
 *  T2 => 洗茶壶 + 洗茶杯 + 拿茶叶
 *
 * T1 执行到「泡茶」时需要依赖 T2 执行完「拿茶叶」才能继续执行，
 * 所以 T1 执行「泡茶」前需要等待 T2 执行完「拿茶叶」才能继续执行，利用 Future 的特性实现同步等待。
 */
class demo{
    // T1 执行「洗水壶 -> 烧热水 -> 泡茶」
    private static class T1Task implements Callable<String>{
        private FutureTask<String> futureTask;
        public T1Task(FutureTask<String> futureTask) {
            this.futureTask = futureTask;
        }
        @Override
        public String call() throws Exception {
            System.out.println("T1：洗水壶");
            TimeUnit.SECONDS.sleep(1);

            System.out.println("T1：烧热水");
            TimeUnit.SECONDS.sleep(15);

            String teaName = futureTask.get();
            System.out.println("T1：拿到茶叶[" + teaName + "]");
            System.out.println("T1：泡 " + teaName + " 茶");
            return "夹碟啦🍵";
        }
    }

    // T2 执行「洗茶壶 -> 洗茶杯 -> 拿茶叶」
    private static class T2Task implements Callable<String>{
        @Override
        public String call() throws Exception {
            System.out.println("T2：洗茶壶");
            TimeUnit.SECONDS.sleep(1);

            System.out.println("T2：洗茶杯");
            TimeUnit.SECONDS.sleep(2);

            System.out.println("T2：拿茶叶");
            TimeUnit.SECONDS.sleep(1);
            return "龙井";
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 创建任务 FutureTask2 执行「洗茶壶 -> 洗茶杯 -> 拿茶叶」
        FutureTask futureTask2 = new FutureTask<>(new T2Task());
        // 创建任务 FutureTask1 执行「洗水壶 -> 烧热水 -> 泡茶」
        FutureTask futureTask1 = new FutureTask<>(new T1Task(futureTask2));

        // 线程 T2 运行任务 FutureTask2
        Thread T2 = new Thread(futureTask2);
        // 线程 T1 运行任务 FutureTask1
        Thread T1 = new Thread(futureTask1);
        T2.start();
        T1.start();

        // 等待线程 T1 的执行结果
        System.out.println(futureTask1.get());
    }
}

/**
 * 电商询价应用
 *  - 从三个电商询价，然后保存在自己的数据库中
 */
class inquiry{

    private static List<String> myDB = new Vector<>();

    public static String getPriceByS1(){
        return "priceS1";
    }
    public static String getPriceByS2(){
        return "priceS2";
    }
    public static String getPriceByS3(){
        return "priceS3";
    }
    public static void save(String price){
        myDB.add(price);
    }

    // 由于是串行操作，性能很慢
    public static void inquiryPrice(){
        // 向电商S1询价，并保存
        String r1 = getPriceByS1();
        save(r1);
        // 向电商S2询价，并保存
        String r2 = getPriceByS2();
        save(r2);
        // 向电商S3询价，并保存
        String r3 = getPriceByS3();
        save(r3);
    }

    // 串行优化为并行，提高性能
    public static void inquiryPriceExecutor(){
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 向电商S1询价，并保存
        Future<?> result1 = executor.submit(new Runnable() {
            @Override
            public void run() {
                save(getPriceByS1());
            }
        });
        // 向电商S2询价，并保存
        Future<?> result2 = executor.submit(new Runnable() {
            @Override
            public void run() {
                save(getPriceByS2());
            }
        });
        // 向电商S3询价，并保存
        Future<?> result3 = executor.submit(new Runnable() {
            @Override
            public void run() {
                save(getPriceByS3());
            }
        });

        while(!(result1.isDone() && result2.isDone() && result3.isDone())){
            System.out.println("result1: " + result1.isDone());
            System.out.println("result2: " + result2.isDone());
            System.out.println("result3: " + result3.isDone());
        }

        executor.shutdown();
    }

    public static void main(String[] args) {
        // inquiryPrice();
        inquiryPriceExecutor();

        myDB.forEach(System.out::println);
    }
}

