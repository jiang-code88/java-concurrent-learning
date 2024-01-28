package com.learn._02_synchronized;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Java 并发包的读写锁 ReadWriteLock
 *  - Java 中已提供管程 synchronized ReentrantLock，信号量 Semaphore
 *    其中任何一个都可以解决所有的并发问题。
 *  - JUC 包中提供的其他工具类目的是 「分场景优化性能，提高易用性」
 *  - ReadWriteLock 对应的普遍并发场景：读多写少场景
 *
 * 读多写少场景：
 *  - 使用缓存，缓存元数据、缓存基础数据。缓存能够提高性能的原因是缓存的数据一定是读多写少的。
 *  - 例如元数据和基础数据基本上不会发生变化（写少），但是使用他们的地方很多（读多）。
 * 场景优化的原则：
 *  1）允许多个线程同时读共享变量；
 *  2）只允许一个线程写共享变量；
 *  3）如果一个写线程正在执行写操作，此时禁止读线程读共享变量。
 * 场景优化的原理：
 *  1）读写锁允许多个线程同时读取共享变量，在互斥锁是不允许的。
 *  2）在读多的环境下，读写锁可以减少繁多读操作的互斥消耗，提高性能。
 *  3）读写锁的写操作是互斥的。当一个线程在写共享变量的时候，是不允许其他线程执行写操作和读操作。
 *
 * 读写锁的升级与降级：
 *  - 读锁不允许升级到写锁
 *  - 写锁允许降级到读锁
 */
public class _14_ReadWriteLock {
    // 实现一个通用的缓存工具类

}

class Cache<K,V>{
    private final Map<K, V> m = new HashMap<>();

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    // 读锁
    private final Lock r = rwl.readLock();

    // 写锁
    private final Lock w = rwl.writeLock();

    // 取缓存
    public V get(K key){
        r.lock();
        try {
            return m.get(key);
        }finally {
            r.unlock();
        }
    }

    // 取缓存-懒加载
    public V getLazy(K key){
        V value = null;
        r.lock();
        try{
            value = m.get(key);
        }finally {
            r.unlock();
        }

        // 缓存命中，返回结果
        if (value != null){
            return value;
        }

        // 缓存未命中，查询数据库
        w.lock();
        try{
            // 再次验证，避免同样拿到写锁的线程，重复查询数据库更新缓存
            value = m.get(key);
            if (value == null){
                // 查询数据库
                value = (V) new Object();
                // 更新缓存
                m.put(key, value);
            }
        }finally {
            w.unlock();
        }
        return value;
    }

    // 写缓存
    public void put(K key, V value){
        w.lock();
        try {
            m.put(key, value);
        }finally {
            w.unlock();
        }
    }
}
