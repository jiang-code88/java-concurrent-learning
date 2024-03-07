package com.learn._02_concurrentUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Fork/Join 的并行计算框架
 *
 * 常见的并发任务模型：
 *  1）简单并行任务
 *  2）聚合任务
 *  3）批量并行任务
 *  4）分治任务：把一个复杂的问题分解成多个相似的子问题，
 *     然后再把子问题分解成更小的子问题，直到子问题简单到可以直接求解。
 *
 * Java 并发包里提供了一种叫做 Fork/Join 的并行计算框架，就是用来支持分治这种任务模型。
 * 分治任务模型可分为两个阶段：
 *  一个阶段是任务分解，也就是将任务迭代地分解为子任务，直至子任务可以直接计算出结果；
 *  另一个阶段是结果合并，即逐层合并子任务的执行结果，直至获得最终结果。
 * 分治任务模型里，任务和分解后的子任务具有相似性，这种相似性往往体现在任务和子任务的算法是相同的，
 * 但是计算的数据规模是不同的。具备这种相似性的问题，我们往往都采用递归算法。
 *
 * Fork/Join 框架中，Fork 对应的是分治任务模型里的任务分解，Join 对应的是结果合并。
 * Fork/Join 计算框架主要包含两部分，一部分是分治任务的线程池 ForkJoinPool，另一部分是分治任务 ForkJoinTask。
 * 这两部分的关系类似于 ThreadPoolExecutor 和 Runnable 的关系，
 * 都可以理解为提交任务到线程池，只不过分治任务有自己独特类型 ForkJoinTask。
 *
 * ForkJoinTask 是一个抽象类，
 * 其中 fork() 方法会异步地执行一个子任务，
 * 而 join() 方法则会阻塞当前线程来等待子任务的执行结果。
 *
 * ForkJoinTask 有两个子类 RecursiveAction 和 RecursiveTask
 * 这两个子类都定义了抽象方法 compute()，区别是
 * RecursiveAction 定义的 compute() 没有返回值，
 * RecursiveTask 定义的 compute() 方法是有返回值的。
 * 这两个子类也是抽象类，在使用的时候，需要你定义子类去扩展。
 *
 * ForkJoinPool 工作原理：
 *  Fork/Join 并行计算框架的核心组件是 ForkJoinPool，
 *  ThreadPoolExecutor 内部只有一个任务队列，而 ForkJoinPool 内部有多个任务队列，
 *  当我们通过 ForkJoinPool 的 invoke() 或者 submit() 方法提交任务时，
 *  ForkJoinPool 根据一定的路由规则把任务提交到一个任务队列中，
 *  如果任务在执行过程中会创建出子任务，那么子任务会提交到工作线程对应的任务队列中。
 *  如果某个工作线程对应的任务队列空了，ForkJoinPool 支持任务窃取机制，
 *  如果工作线程空闲了，那它可以“窃取”其他工作任务队列里的任务，
 *  同时 ForkJoinPool 中的任务队列采用的是双端队列，
 *  工作线程正常获取任务和 “窃取任务” 分别是从任务队列不同的端消费，这样能避免很多不必要的数据竞争。
 *  因此 ForkJoinPool 能够让所有线程的工作量基本均衡，
 *  不会出现有的线程很忙，而有的线程很闲的状况，所以性能很好。
 *
 *  Java 1.8 提供的 Stream API 里面并行流也是以 ForkJoinPool 为基础的。
 *  不过需要你注意的是，默认情况下所有的并行流计算都共享一个 ForkJoinPool，
 *  这个共享的 ForkJoinPool 默认的线程数是 CPU 的核数；
 *  如果所有的并行流计算都是 CPU 密集型计算的话，完全没有问题，
 *  但是如果存在 I/O 密集型的并行流计算，
 *  那么很可能会因为一个很慢的 I/O 计算而拖慢整个系统的性能。
 *  所以建议用不同的 ForkJoinPool 执行不同类型的计算任务。
 */

/**
 * 1 用 Fork/Join 这个并行计算框架计算斐波那契数列
 */
class Fibonacci extends RecursiveTask<Integer> {
    final int n;

    // 定义算到 index 到那几位的菲波那契数
    public Fibonacci(int n) {
        this.n = n;
    }

    // 实现 RecursiveTask 的 computer 方法，定义分治任务的具体操作
    @Override
    protected Integer compute() {
        // 递归递出条件
        if (n <= 1){
            return n;
        }
        Fibonacci f1 = new Fibonacci(n - 1);
        // 创建异步执行的子任务 f1，异步的执行任务 f1 的计算
        f1.fork();
        Fibonacci f2 = new Fibonacci(n - 2);
        // 主线程负责继续执行任务 f2，同时等待异步子任务 f1 的执行，最后执行 + 的合并操作
        return f2.compute() + f1.join();
    }

    public static void main(String[] args) {
        // 创建分治任务线程池
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        // 创建分治任务
        Fibonacci forkJoinTask = new Fibonacci(6);
        // 分治任务线程池启动分治任务
        Integer result = forkJoinPool.invoke(forkJoinTask);
        // 输出结果
        // value：0 1 1 2 3 5「8」
        // index：0 1 2 3 4 5 6
        System.out.println(result);
    }
}

/**
 * 2 模拟 MapReduce 统计单词数量
 *
 * 需求：统计一个文件里面每个单词的数量。
 * 方案：我们可以先用二分法递归地将一个文件拆分成更小的文件，
 *      直到文件里只有一行数据，然后统计这一行数据里单词的数量，最后再逐级汇总结果。
 */
public class _13_ForkJoinMapReduce {

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
        MapReduce mapReduceTask = new MapReduce(file, 0, file.length);
        // 启动分治任务
        Map<String, Integer> result = forkJoinPool.invoke(mapReduceTask);
        // 输出结果
        result.forEach((k,v)->{ System.out.println(k + " : " + v); });
        // hello：4
        // world：2
        // me：1
        // fork：2
        // join：2
        // in：1
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

        // 统计单行的单词数量
        public Map<String, Integer> calc(String line){
            Map<String, Integer> map = new HashMap<>();
            // 以一个或多个空格进行单词切分
            String[] words = line.split("\\s+");
            for (String word : words) {
                map.merge(word, 1, Integer::sum);
            }
            // 一行中每个单词的出现次数
            return map;
        }

        // 合并结果
        public Map<String, Integer> merge(Map<String, Integer> map1, Map<String, Integer> map2){
            Map<String, Integer> mergeMap = new HashMap<>();
            // 选择数量少的 map 进行遍历合并，减少便利次数，提高性能
            Map<String, Integer> minSizeMap = map1;
            Map<String, Integer> maxSizeMap = map2;
            if (minSizeMap.size() > maxSizeMap.size()){
                minSizeMap = map2;
                maxSizeMap = map1;
            }
            mergeMap.putAll(maxSizeMap);
            minSizeMap.forEach((k,v)->{
                mergeMap.merge(k, v, Integer::sum);
            });
            return mergeMap;
        }
    }
}