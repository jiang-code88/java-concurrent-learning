package com.learn._02_concurrentUtils;

import com.learn.common.CommTools;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Java 1.8 提供了 CompletableFuture 工具类来支持异步编程，功能十分强大。
 *
 * 用多线程优化性能，其实不过就是将串行操作变成并行操作，
 * 而在串行转换成并行的过程中，一定会涉及到异步化
 */
public class _11_CompletableFuture {
    public static void main(String[] args) {

        /* CompletableFuture 的使用指导 */

        // 1 创建 CompletableFuture 对象（表示一个异步任务）
        // CompletableFuture<Void> runAsync(Runnable runnable)
        // <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier)
        //  - Runnable 和 Supplier 接口用于定义异步任务的实际逻辑。
        //  - 他们之间的区别是 Runnable 接口的 run() 方法没有返回值，而 Supplier 接口的 get() 方法是有返回值的。

        // CompletableFuture<Void> runAsync(Runnable runnable, Executor executor)
        // <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        //  - 这两个方法可以指定线程池参数。
        //  - 默认情况下 CompletableFuture 会使用公共的 ForkJoinPool 线程池，
        //    这个线程池默认创建的线程数是 CPU 的核数（可以通过 JVM option:-Djava.util.concurrent.ForkJoinPool.common.parallelism 来设置 ForkJoinPool 线程池的线程数）。
        //  - 如果所有 CompletableFuture 共享一个线程池，那么一旦有任务执行一些很慢的 I/O 操作，
        //    就会导致线程池中所有线程都阻塞在 I/O 操作上，从而造成线程饥饿，进而影响整个系统的性能。
        //    所以，强烈建议你要根据不同的业务类型创建不同的线程池，以避免互相干扰。

        // 2 创建完 CompletableFuture 对象之后，会自动地异步执行 runnable.run() 方法或者 supplier.get() 方法

        // 3 由于 CompletableFuture 类实现了 Future 接口，
        //   所以可以通过 CompletableFuture 对象：1.知晓异步任务什么时候结束 2.获取异步任务的执行结果。

        // 4 CompletableFuture 对象实现 CompletionStage 接口提供的功能
        //   从任务的角度分析工作流：任务存在的时序关系
        //   1）串行角度
        //   2）并行角度
        //   3）汇聚角度：AND 聚合关系 / OR 聚合关系
        // CompletionStage 接口中的方法用于清晰的描述任务之间的时序关系

        // 4.1 描述任务串行关系
        // <U> CompletableFuture<U> thenApply(Function<? super T,? extends U> fn)
        //  - Function<T, R> 接口对应的方法为 R apply(T t); 既能接收参数也支持返回值。
        // CompletableFuture<Void> thenAccept(Consumer<? super T> consumer)
        //  - Consumer<T> 接口对应的方法是 void accept(T t); 虽然支持参数，但却不支持回值。
        // CompletableFuture<Void> thenRun(Runnable action)
        //  - Runnable 接口对应的方法是 run(); 既不能接收参数也不支持返回值。
        // <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn)
        //  - 该方法会新创建出一个子流程，但最终结果和 thenApply 方法是相同的。
        // 如果方法带有 Async 后缀代表的是异步执行 fn、consumer 或者 action，即不是在调用方法的线程中执行的，另开一个线程。
        // apply(); // 使用示例

        // 4.2 描述 AND 汇聚关系
        // <U,V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,
        //         BiFunction<? super T,? super U,? extends V> fn)
        // <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
        //         BiConsumer<? super T, ? super U> action)
        // CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action)

        // 4.3 描述 OR 汇聚关系
        // <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn)
        // CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action)
        // CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action)
        // either(); // 使用示例

        // 5 异常处理
        // CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn)
        //  - 使用类似于 try{}catch{} 中的 catch{}
        // CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action)
        // <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
        //  - whenComplete() 和 handle() 使用类似于 try{}finally{} 中的 finally{}
        //    无论是否发生异常都会执行 whenComplete() 中的回调函数 consumer 和 handle() 中的回调函数 fn
        //    区别在于 whenComplete() 不支持返回结果，handle() 是支持返回结果的。
        // exception(); // 使用示例
    }

    public static void apply(){
        // 首先通过 supplyAsync() 启动一个异步流程，之后是两个串行操作，
        // 虽然这是一个异步流程，但任务①②③④却是串行执行的，②依赖①的执行结果，③依赖②的执行结果，④依赖于③的执行结果。
        CompletableFuture<String> f0 =
                CompletableFuture.supplyAsync(() -> "Hello World") // ①
                        .thenApply(s -> s + " QQ")                 // ②
                        .thenApply(s -> s + " WeChat")             // ③
                        .thenApply(String::toUpperCase);           // ④
        System.out.println(f0.join()); // 输出结果：HELLO WORLD QQ
    }

    public static void either(){
        CompletableFuture<String> f1 =
                CompletableFuture.supplyAsync(()->{
                    int t = CommTools.getRandom(5, 10);
                    CommTools.sleep(t, TimeUnit.SECONDS);
                    return "f1：" + t;
                });

        CompletableFuture<String> f2 =
                CompletableFuture.supplyAsync(()->{
                    int t = CommTools.getRandom(5, 10);
                    CommTools.sleep(t, TimeUnit.SECONDS);
                    return "f2：" + t;
                });

        // f1 或者 f2 其中一个执行完成后会使用返回结果执行 f3
        CompletableFuture<String> f3 = f1.applyToEither(f2, s -> s + " SECONDS");
        System.out.println(f3.join());
    }

    public static void exception(){
        CompletableFuture<Integer>
                f0 = CompletableFuture
                .supplyAsync(()->(7/0)) // 执行 7/0 就会出现除零错误这个运行时异常
                .thenApply(r->r*10)
                .exceptionally(e->{ // exceptionally() 的使用非常类似于 try{}catch{}中的 catch{}
                    System.out.println(e.getMessage());
                    return 7;
                });
        System.out.println(f0.join());
    }

}

/**
 * 使用 CompletableFuture 改写「烧水泡茶」程序
 *
 * 场景：
 * 洗水壶1m --> 烧热水15m --> 泡茶
 *             洗茶壶1m
 *             洗茶杯2m
 *             拿茶叶1m
 * 分工方案：
 *  T1 => 洗水壶 + 烧热水
 *  T2 => 洗茶壶 + 洗茶杯 + 拿茶叶
 *  T3 => 泡茶
 * 线程 T1 和 T2 可以并行运行，T3 的运行依赖于 T1 和 T2 的运行结束。
 *
 * CompletableFuture 的核心优势：
 *  1）无需手工维护线程，没有繁琐的手工维护线程的工作，给任务分配线程的工作也不需要我们关注；
 *  2）语义更清晰，例如 f3 = f1.thenCombine(f2, ()->{}) 能够清晰地表述“任务 3 要等待任务 1 和任务 2 都完成后才能开始”；
 *  3）代码更简练并且专注于业务逻辑，几乎所有代码都是业务逻辑相关的。
 */
class BoilingWaterBrewTea{
    public static void main(String[] args) {
        // T1（任务一）：洗水壶 -> 烧开水
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(()->{
            System.out.println("T1:洗水壶...");
            CommTools.sleep(1, TimeUnit.SECONDS);

            System.out.println("T1:烧开水...");
            CommTools.sleep(15, TimeUnit.SECONDS);
        });

        // T2（任务二）：洗茶壶 -> 洗茶杯 -> 拿茶叶
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(()->{
            System.out.println("T2:洗茶壶...");
            CommTools.sleep(1, TimeUnit.SECONDS);

            System.out.println("T2:洗茶杯...");
            CommTools.sleep(2, TimeUnit.SECONDS);

            System.out.println("T2:拿茶叶...");
            CommTools.sleep(1, TimeUnit.SECONDS);
            return "龙井";
        });

        // T3（任务三）：任务 1 和任务 2 完成后执行 -> 泡茶
        // 参数一传入任务 f1 的返回值，参数二传入任务 f2 的返回值。由于任务 f1 没有返回值所以使用 __ 表示省略传参数。
        CompletableFuture<String> f3 = f1.thenCombine(f2, (__, teaName)->{
            System.out.println("T3:拿到茶叶:" + teaName);
            System.out.println("T3:泡茶...");
            return "上茶:" + teaName;
        });

        // 主线程等待任务三执行完成，打印任务三的执行结果
        System.out.println(f3.join());
    }
}
