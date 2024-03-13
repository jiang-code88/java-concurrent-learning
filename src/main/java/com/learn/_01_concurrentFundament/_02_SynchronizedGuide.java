package com.learn._01_concurrentFundament;


/**
 * Java 提供的互斥锁 synchronized，用于解决原子性问题
 * 1. synchronized 关键字可以修饰方法也可以修饰代码块
 * 2. Java 编译器会在 synchronized 修饰的方法或代码块前后自动加上加锁 lock() 和解锁 unlock()
 */
public class _02_SynchronizedGuide {
    // 1. 修饰非静态方法
    // 当修饰非静态方法的时候，锁定的是当前实例对象 this
    public synchronized void foo(){
        // 临界区
    }

    // 2. 修饰静态方法
    // 当修饰静态方法的时候，锁定的是当前类的 Class 对象，也就是 _02_SynchronizedGuide.class
    public synchronized static void bar(){
        // 临界区
    }

    // 3. 修饰代码块(锁定实例对象)
    public Object obj = new Object();
    public void foa(){
        // 对代码块加锁，锁定实例对象
        synchronized (obj){
            // 临界区
        }
    }

    // 4. 修饰代码块(锁定类的 Class 对象)
    public void baz(){
        // 对代码块加锁，锁定类的 Class 对象
        synchronized (_02_SynchronizedGuide.class){
            // 临界区
        }
    }
}
