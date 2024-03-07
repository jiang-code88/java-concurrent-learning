package com.learn._02_concurrentUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lock & Condition 实现的管程是支持多个条件变量的，
 * 而 synchronized 只支持一个条件变量。
 * - 在很多并发场景下，支持多个条件变量能够让我们的并发程序可读性更好，实现起来也更容易
 *
 * 同步和异步：
 *  - 调用方如果需要等待结果，就是同步。（Java 代码默认的处理方式）
 *  - 调用方如果不需要等待结果，就是异步。
 *
 * 实现异步的两种方式
 *  1. 调用方创建一个子线程，在子线程中执行方法调用，这种调用我们称为异步调用；
 *  2. 方法实现的时候，创建一个新的线程执行主要逻辑，主线程则直接 return，这种方法我们一般称为异步方法。
 *
 * 使用 Lock & Condition 可以实现异步转同步，转换的目的是同步的 API 更易用。
 */
public class _02_LockCondition_2 {

    /**
     * 场景：TCP 协议本身就是异步的，我们工作中经常用到的 RPC 调用，在 TCP 协议层面，
     *      发送完 RPC 请求后，线程是不会等待 RPC 的响应结果的。
     *      但是我们希望改成更易用的同步调用方式。
     * 发送 TCP 请求后，同步阻塞等待响应的返回，响应返回后再唤醒继续执行，同步阻塞支持超时时间。
     */

    private Lock lock = new ReentrantLock();

    private Condition done = lock.newCondition();

    Object response = null;

    public Object get(long timeout) throws TimeoutException, InterruptedException {
        long start = System.currentTimeMillis();
        lock.lock();
        try{
            while (!isDone()){
                done.await(timeout, TimeUnit.MICROSECONDS);
                // 被唤醒后有三种情况：
                // 1. 收到响应 -> 直接返回
                // 2. 没有收到响应，未超时间 -> 继续陷入阻塞
                // 3. 没有收到响应，超时 -> 错误返回
                long cur = System.currentTimeMillis();
                if (isDone() || cur - start > timeout){
                    break;
                }
            }
        } finally {
            lock.unlock();
        }

        // 线程等待响应超时，抛出的超时异常
        if (!isDone()){
            throw new TimeoutException();
        }

        return response;
    }

    public boolean isDone(){
        return response != null;
    }

    public void doReceived(Object resp){
        lock.lock();
        try {
            if (response != null){
                response = resp;
                done.signalAll();
            }
        }finally {
            lock.unlock();
        }
    }

}
