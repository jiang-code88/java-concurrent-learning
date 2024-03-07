package com.learn._03_concurrentDesignPattern;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Balking 模式：
 *  - 业务逻辑依赖于某个状态变量的状态：当状态满足某个条件时，执行某个业务逻辑，
 *    其本质其实不过就是一个 if 而已，放到多线程场景里，就是一种 “多线程版本的 if”。
 *
 * Balking 模式本质上是一种规范化地解决 “多线程版本的 if” 的方案：
 *  - 多线程的代码根据某个状态变量选择是否执行具体逻辑，如果状态变量符合执行，不符合快速失败不执行。
 *
 * Balking模式 和 Guarded Suspension模式对比：
 *  - 相同：解决线程安全的if。
 *  - 不同点： Balking模式：使用互斥锁，不等待 if 条件为真，失败直接放弃执行。
 *           Guarded Suspension模式：使用管程，需要等待 if 条件为真，失败会阻塞等待条件满足。
 *
 * Balking 模式的经典实现是使用互斥锁，你可以使用 Java 语言内置 synchronized，
 * 也可以使用 SDK 提供 Lock；如果你对互斥锁的性能不满意，可以尝试采用 volatile 方案，
 * 不过使用 volatile 方案需要你更加谨慎，避免存在原子性问题的影响。
 * 此外也可以尝试使用双重检查方案来优化性能，
 * 双重检查中的第一次检查，完全是出于对性能的考量：避免执行加锁操作，因为加锁操作很耗时。
 * 而加锁之后的二次检查，则是出于对安全性负责，避免对象被重复创建。
 */
public class _05_Balking {

}

// 应用场景一：实现单次初始化
class Initialize{
    // 是否已初始化标志
    private boolean inited = false;

    // 单次初始化函数
    //  - synchronized 关键字保证同一时刻只有一个线程可以执行 init() 方法
    //  - init() 方法第一次执行时会将 inited 标志置为 true
    //    这样后续执行 init() 方法的线程将不会再次执行 doInit() 方法进行初始化了
    public synchronized void init(){
        if (inited){
            return;
        }
        doInit();
        inited = true;
    }
    // 执行具体的初始化操作
    public void doInit(){

    }
}

// 应用场景二：线程安全的单例模式
class Singleton{
    private static Singleton singleton;

    // 构造函数私有化
    private Singleton(){}

    // 获取实例（单例）对象
    // - 但是这种情况的性能很差，因为互斥锁把 getInstance() 方法的执行串行化了
    public synchronized static Singleton getInstance(){
        if (singleton == null){
            singleton = new Singleton();
        }
        return singleton;
    }
}

// 应用场景二：改进线程安全的单例模式（使用双重检验优化性能）
class SingleTonDoubleCheck{
    // 使用了 volatile 来禁止编译优化带来的指令重排
    private volatile static SingleTonDoubleCheck singleton;
    private SingleTonDoubleCheck(){}

    // 获取实例（单例）
    // - 当单例对象被创建后，后续执行 getInstance 方法的线程都是无锁的，能够提高性能。
    // - 第一次检验是无锁的为了提高性能，第二次检验是为了安全性保证对象只被创建一次。
    // - 为什么就双重检验要特别使用 volatile 保证原子性呢？
    //   1）首先 new 这个操作不是原子性操作，分三步：1.分配空间，2.实例化对象 3.将地址赋值变量
    //   2）由于有没有锁的检验 if，一旦出现执行重排 1 3 2，执行到步骤 2 时发生线程切换，
    //     看似判断到对象不为 null 但实际上此时对象还没实例化初始化好，存在有问题，所以就需要特殊的进行禁止指令重排。
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

// 应用场景三：仅仅计算一次 count 值
class CalcOnce{
    private int count = 0;

    // 双重检验保证实现
    private volatile boolean inited = false;
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

    // 使用 AtomicBoolean 替换 inited 标志进行更新判断
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

    private int calc(){
        return 101;
    }
}


