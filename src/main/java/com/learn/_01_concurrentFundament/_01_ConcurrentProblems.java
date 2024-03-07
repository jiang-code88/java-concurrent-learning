package com.learn._01_concurrentFundament;

import com.learn.common.CommTools.baseMethod;

/* 重现可见性问题 */
class ProblemVisibility implements baseMethod {
    // 多线程共享变量（共享资源）
    private static int count = 0;

    // 对共享变量累加到一亿的操作
    @Override
    public void addOneHundredMillion(){
        final int oneHundredMillion = 100_000_000;
        for (int i = 0; i < oneHundredMillion; i++) {
            count += 1;
        }
    }

    @Override
    public int getResult() {
        return count;
    }

    @Override
    public int resetResult() {
        return count = 0;
    }

    public static void main(String[] args) throws InterruptedException {
        _01_ConcurrentProblems.testProgram(new ProblemVisibility());
    }
}

/* 重现原子性问题 */
class ProblemAtomic implements baseMethod {
    // 多线程共享变量（共享资源）
    // 使用 volatile 关键字修改线程的共享变量，禁用 CPU 缓存，解决可见性问题
    private static volatile int count = 0;

    // 对共享变量累加到一亿的操作
    @Override
    public void addOneHundredMillion(){
        final int oneHundredMillion = 100_000_000;
        for (int i = 0; i < oneHundredMillion; i++) {
            count += 1;
        }
    }

    @Override
    public int getResult() {
        return count;
    }

    @Override
    public int resetResult() {
        return count = 0;
    }

    public static void main(String[] args) throws InterruptedException {
        _01_ConcurrentProblems.testProgram(new ProblemAtomic());
    }
}

/* 解决原子性和可见行问题 */
class ProblemResolve implements baseMethod{
    private static int count = 0;

    @Override
    public void addOneHundredMillion() {
        final int oneHundredMillion = 100_000_000;
        for (int i = 0; i < oneHundredMillion; i++) {
            synchronized (this){
                count += 1;
            }
        }
    }

    @Override
    public int getResult() {
        return count;
    }

    @Override
    public int resetResult() {
        return count = 0;
    }

    public static void main(String[] args) throws InterruptedException {
        _01_ConcurrentProblems.testProgram(new ProblemResolve());
    }
}

public class _01_ConcurrentProblems {
    public static void testProgram(baseMethod task)
            throws InterruptedException {
        while (true){
            System.out.println("==================================");
            // 两个线程并发的对共享变量进行累加操作，预期是将共享变量累加到两亿
            Thread t1 = new Thread(task::addOneHundredMillion);
            Thread t2 = new Thread(task::addOneHundredMillion);
            // 启动线程
            t1.start();
            t2.start();
            // main 主线程等待两个子线程执行完毕再继续执行
            t1.join();
            t2.join();
            // 打印累加的共享变量结果
            System.out.println("result: [" + task.getResult() + "]");
            System.out.println("==================================\n");
            // 如果结果不为预期的两亿，结束程序的运行
            if (task.getResult() != 200_000_000){
                break;
            }
            // 如果结果为预期的两亿，继续运行程序，目的是把线程不安全问题重现出来
            // 重置结果, 供下一次多线程操作结果展示使用
            task.resetResult();
        }
    }
}
