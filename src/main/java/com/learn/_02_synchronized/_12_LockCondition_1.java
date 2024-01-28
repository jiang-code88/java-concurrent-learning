package com.learn._02_synchronized;

/**
 * Lock & Condition 实现的管程是支持多个条件变量的，而 synchronized 只支持一个条件变量
 *
 * 同步和异步：
 *  - 调用方如果需要等待结果，就是同步。（Java 代码默认的处理方式）
 *  - 调用方如果不需要等待结果，就是异步。
 *
 * 实现异步的两种方式
 *  1. 调用方创建一个子线程，在子线程中执行方法调用，这种调用我们称为异步调用；
 *  2. 方法实现的时候，创建一个新的线程执行主要逻辑，主线程直接 return，这种方法我们一般称为异步方法。
 *
 * Lock & Condition 可以实现异步转同步
 */
public class _12_LockCondition_1 {
}
