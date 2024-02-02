package com.learn._02_threadAndTools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Fork/Join 的并行计算框架
 *
 * 对于简单的并行任务，你可以通过“线程池 +Future”的方案来解决；
 * 如果任务之间有聚合关系，无论是 AND 聚合还是 OR 聚合，都可以通过 CompletableFuture 来解决；
 * 而批量的并行任务，则可以通过 CompletionService 来解决。
 *
 * 并发任务模型：
 * 1）简单并行任务
 * 2）聚合任务
 * 3）批量并行任务
 * 4）分治任务
 *   把一个复杂的问题分解成多个相似的子问题，然后再把子问题分解成更小的子问题，直到子问题简单到可以直接求解。
 *
 * Java 并发包里提供了一种叫做 Fork/Join 的并行计算框架，就是用来支持分治这种任务模型。
 * 分治任务模型可分为两个阶段：
 *  一个阶段是任务分解，也就是将任务迭代地分解为子任务，直至子任务可以直接计算出结果；
 *  另一个阶段是结果合并，即逐层合并子任务的执行结果，直至获得最终结果。
 * 分治任务模型里，任务和分解后的子任务具有相似性，这种相似性往往体现在任务和子任务的算法是相同的，
 * 但是计算的数据规模是不同的。具备这种相似性的问题，我们往往都采用递归算法。
 *
 *  Fork 对应的是分治任务模型里的任务分解，Join 对应的是结果合并。
 *
 * ForkJoinTask 有两个子类——RecursiveAction 和 RecursiveTask
 * 这两个子类都定义了抽象方法 compute()，区别是
 * RecursiveAction 定义的 compute() 没有返回值，
 * RecursiveTask 定义的 compute() 方法是有返回值的。
 * 这两个子类也是抽象类，在使用的时候，需要你定义子类去扩展。
 *
 * Fork/Join 并行计算框架的核心组件是 ForkJoinPool。
 * ForkJoinPool 支持任务窃取机制，能够让所有线程的工作量基本均衡，
 * 不会出现有的线程很忙，而有的线程很闲的状况，所以性能很好。
 * Java 1.8 提供的 Stream API 里面并行流也是以 ForkJoinPool 为基础的。
 * 不过需要你注意的是，默认情况下所有的并行流计算都共享一个 ForkJoinPool，
 * 这个共享的 ForkJoinPool 默认的线程数是 CPU 的核数；
 * 如果所有的并行流计算都是 CPU 密集型计算的话，完全没有问题，
 * 但是如果存在 I/O 密集型的并行流计算，
 * 那么很可能会因为一个很慢的 I/O 计算而拖慢整个系统的性能。
 * 所以建议用不同的 ForkJoinPool 执行不同类型的计算任务。
 */
public class _23_ForkJoinMapReduce {
    // 2 模拟 MapReduce 统计单词数量
    public static void main(String[] args) {
        // 要统计的文本文件
        String[] file = {
                "hello world",
                "hello me",
                "hello fork",
                "hello join",
                "fork join in world"
        }; // hello：4，world：2，me：1，fork：2，join：2，in：1

        // 创建分治线程池
        ForkJoinPool forkJoinPool = new ForkJoinPool(3);
        // 创建分治任务
        MapReduce mapReduce = new MapReduce(file, 0, file.length);
        // 启动分治任务
        Map<String, Integer> result = mapReduce.invoke();
        // 输出结果
        result.forEach((k,v)->{
            System.out.println(k + " : " + v);
        });
    }
    // 模仿 MapReduce 操作
    static class MapReduce extends RecursiveTask<Map<String, Integer>>{
        String [] file;
        int start;
        int end;
        public MapReduce(String[] file, int start, int end) {
            this.file = file;
            this.start = start;
            this.end = end;
        }
        @Override
        protected Map<String, Integer> compute() {
            // 递归递出条件
            if (end - start == 1){
                // 计算单行内容的单次数量
                return calc(file[start]);
            }

            int mid = (start + end) / 2;

            MapReduce f1 = new MapReduce(file, start, mid);
            f1.fork();
            MapReduce f2 = new MapReduce(file, mid, end);

            return merge(f2.compute(), f1.join());
        }

        public Map<String, Integer> calc(String line){
            Map<String, Integer> map = new HashMap<>();
            String[] words = line.split("\\s+");
            for (String word : words) {
                map.merge(word, 1, Integer::sum);
            }
            return map;
        }

        public Map<String, Integer> merge(Map<String, Integer> mr1, Map<String, Integer> mr2){
            Map<String, Integer> mergeMap = new HashMap<>();
            // 选择数量少的 map 进行遍历，提高性能
            Map<String, Integer> minSizeMap = mr1;
            Map<String, Integer> maxSizeMap = mr2;
            if (minSizeMap.size() > maxSizeMap.size()){
                minSizeMap = mr2;
                maxSizeMap = mr1;
            }
            mergeMap.putAll(maxSizeMap);
            minSizeMap.forEach((k,v)->{
                mergeMap.merge(k, v, Integer::sum);
            });
            return mergeMap;
        }
    }
}

/**
 * 1 用 Fork/Join 这个并行计算框架计算斐波那契数列
 */
class Fibonacci extends RecursiveTask<Integer> {
    int n;
    public Fibonacci(int n) {
        this.n = n;
    }
    @Override
    protected Integer compute() {
        // 递归递出条件
        if (n <= 1){
            return n;
        }
        Fibonacci f1 = new Fibonacci(n - 1);
        // 创建异步执行的子任务 f1
        f1.fork();
        Fibonacci f2 = new Fibonacci(n - 2);
        // 主线程继续执行任务 f2，等待异步子任务 f1 的执行，然后执行 + 的合并操作
        return f2.compute() + f1.join();
    }

    public static void main(String[] args) {
        // 创建分治任务线程池
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        // 创建分治任务
        Fibonacci task = new Fibonacci(6);
        // 启动分治任务
        Integer result = task.invoke();
        // 输出结果
        System.out.println(result);
    }
}
