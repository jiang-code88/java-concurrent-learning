package com.learn.common;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CommTools {

    /**
     * 封装 Thread.sleep() 睡眠操作，统一处理 InterruptedException 异常。
     * @param t 睡眠时间
     * @param u 时间单位
     */
    public static void sleep(long t, TimeUnit u){
        try {
            u.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取下界 lower 到上界 upper 范围内的随机整数
     * @param lower 下界
     * @param upper 上界
     * @return 随机整数
     */
    public static int getRandom(int lower, int upper){
        Random random = new Random();
        return random.nextInt(upper - lower + 1) + lower;
    }

    /**
     * 计算操作执行时常
     * @param action 执行的操作
     */
    public static void countTimeExec(Runnable action){
        long startTime = System.currentTimeMillis();
        action.run();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("程序执行时间：" + executionTime + " ms");
    }

    /* 重现线程不安全问题时方法重用接口 */
    public interface baseMethod{
        void addOneHundredMillion();
        int getResult();
        int resetResult();
    }
}


