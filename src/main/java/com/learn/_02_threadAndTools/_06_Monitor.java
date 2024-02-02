package com.learn._02_threadAndTools;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Java 中对「管程」思想的实现
 *  - synchronized 关键字及 wait()、notify()、notifyAll() 这三个方法都是管程的组成部分
 *  - 管程和信号量是等价的，所谓等价指的是用管程能够实现信号量，也能用信号量实现管程。
 *    但是管程更容易使用，所以 Java 选择了管程。
 */
public class _06_Monitor {
}

/**
 * 用管程实现一个线程安全的阻塞队列
 * @param <T>
 */
class BlockedQueue<T>{
    private final Lock lock = new ReentrantLock();

    // 条件变量：队列不满（允许往队列放）
    private final Condition notFull = lock.newCondition();

    // 条件变量：队列不空（允许从队列取）
    private final Condition notEmpty = lock.newCondition();

    // 入队
    public void enq(T e) throws InterruptedException {
        lock.lock();
        try{
            // while(/*队列已满*/){
            //     notFull.await();
            // }
            // 省略入队具体操作...
            // 入队后，通知其他线程可出队
            notEmpty.signalAll();
        }finally {
            lock.unlock();
        }
    }

    // 出队
    public T dep() throws InterruptedException {
        lock.lock();
        try{
            // while (/*队列为空*/){
            //     notEmpty.await();
            // }
            // 省略出队具体操作...
            // 出队后，通知其他线程可入队
            notFull.signalAll();
        }finally {
            lock.unlock();
        }
        return null;
    }
}