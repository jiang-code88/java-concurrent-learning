package com.learn._03_designPattern;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Balking 模式
 */
public class _05_Balking {

}

// 例子一：单次初始化
class InitTest{
    // 是否已初始化标志
    private boolean inited = false;

    // 单次初始化函数
    //  - 同一时刻只有一个线程可以执行 init() 方法
    //  - init() 方法第一次执行时会将 inited 标志置为 true
    //    这样后续执行 init() 方法的线程将不会再次执行 doInit() 方法进行初始化了
    public synchronized void init(){
        if (inited){
            return;
        }
        doInit();
        inited = true;
    }

    public void doInit(){

    }
}

// 例子二：线程安全的单例模式
class Singleton{
    private static Singleton singleton;

    // 构造函数私有化
    private Singleton(){}

    // 获取实例(单例)
    // - 但是这种情况的性能很差，因为互斥锁把 getInstance() 方法的执行串行化了
    public synchronized static Singleton getInstance(){
        if (singleton == null){
            singleton = new Singleton();
        }
        return singleton;
    }
}

// 例子三：线程安全的单例模式（使用双重检验优化性能）
class SingleTonDoubleCheck{
    // 使用了 volatile 来禁止编译优化带来的指令重排
    private volatile static SingleTonDoubleCheck singleton;
    private SingleTonDoubleCheck(){}

    // 获取实例（单例）
    // - 当单例对象被创建后，后续执行 getInstance 方法的线程就是无锁的，提高性能。
    // - 第一次检验是无锁的为了提高性能，第二次检验是为了安全性保证对象只被创建一次。
    // - 为什么就双重检验要特别使用 volatile 保证原子性呢？
    //   1）首先 new 这个操作不是原子性操作，分三步：1.分配空间，2.实例化对象 3.将地址赋值变量
    //   2）由于有没有锁的检验 if，一旦出现执行重排 1 3 2，看似对象不为 null 但实际上对象还没准备好，
    //      存在有问题就要特殊的进行禁止指令重排
    public static SingleTonDoubleCheck getInstance(){
        if (singleton == null){
            synchronized (SingleTonDoubleCheck.class){
                if (singleton == null){
                    singleton = new SingleTonDoubleCheck();
                }
            }
        }
        return singleton;
    }
}

// 例子三：仅仅计算一次 count 值
class Test{
    private volatile boolean inited = false;
    private int count = 0;

    // 双重检验保证
    public void init(){
        if (inited){
            return;
        }
        synchronized (this){
            if (inited){
                return;
            }
            count = calc();
            inited = true;
        }
    }

    private int calc(){
        return 101;
    }


    private AtomicBoolean flag = new AtomicBoolean();
    public void initAtom(){
        if (flag.get()){
            return;
        }

        // 同一时刻，只有一个线程可以执行成功，其他线程退出
        if (!flag.compareAndSet(false, true)){
            return;
        }

        count = calc();
    }
}


