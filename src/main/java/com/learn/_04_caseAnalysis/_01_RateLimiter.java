package com.learn._04_caseAnalysis;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高性能限流器 Guava RateLimiter
 * - Guava 是 Google 开源的 Java 类库
 * -
 */
public class _01_RateLimiter {

    public static void main(String[] args) {
        rateLimiterDemo();
    }

    /**
     * 限流器的使用，任务提交到线程池执行按照 2 个请求 / 秒进行限流，
     * 也就是请求之间间隔 500 毫秒提交执行。
     */
    public static void rateLimiterDemo(){
        // 创建限流器（限流器流速：2 个请求/秒）
        RateLimiter limiter = RateLimiter.create(2.0);
        // 执行任务的线程池
        ExecutorService es = Executors.newFixedThreadPool(1);
        // 记录上一次执行时间
        AtomicLong prev = new AtomicLong(System.nanoTime());
        // 测试执行 20 次
        for (int i = 0; i < 20; i++) {
            // 限流器限流
            limiter.acquire();
            // 提交任务异步执行
            int finalI = i;
            es.execute(()->{
                long cur = System.nanoTime();
                // 打印时间间隔：毫秒
                System.out.println( finalI + "-" +
                        (cur - prev.get()) / 1000_000);
                prev.set(cur);
            });
        }
    }
}

/**
 * 令牌桶容量为 1 的令牌桶算法模拟
 */
class SimpleLimiter{
    // 下一令牌产生时间
    long next = System.nanoTime();

    // 发放令牌的间隔：纳秒
    // 限流到每秒执行一个任务
    long interval = 1000_000_000;

    // 预占令牌，返回能够获取令牌的时间
    public synchronized long reserve(long now){
        /* 请求时间在令牌生成后 */
        if (now > next){
            next = now;
        }
        /* 请求时间在令牌生成前 */
        // 能够获取令牌的时间
        long at = next;
        // 设置下一个令牌的时间
        next += interval;
        // 返回能够获取令牌的时间
        return Math.max(at, 0L);
    }

    // 申请令牌
    public void acquire(){
        // 申请令牌的时间（请求时间）
        long now = System.nanoTime();
        // 预占令牌
        long at = reserve(now);
        long waitTime = Math.max(at - now, 0L);
        // 按照条件等待
        // 请求时间在令牌生成前，at > now 需要需要等待一定时间到 next
        // 请求时间在令牌生成后，at == now 不需要等待马上可以拿到
        if (waitTime > 0){
            try {
                TimeUnit.SECONDS.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}


