package com.learn._02_concurrentUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Java 并发包的读写锁 ReadWriteLock
 *  - Java 中已提供管程 synchronized ReentrantLock，信号量 Semaphore
 *    其中任何一个都可以解决所有的并发问题。
 *  - JUC 包中提供的其他工具类目的是：「分场景优化性能，提高易用性」
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
 *  - 读锁不允许升级到写锁：会出现读锁还没有释放，此时获取写锁，会导致写锁永久等待，
 *      最终导致相关线程都被阻塞，永远也没有机会被唤醒。
 *  - 写锁允许降级到读锁
 *
 * 读写锁存在的意义：
 *  - 读锁是不是没有意义，反正都是读取数据（读到新值、旧值都是一样读），
 *    又不会进行数据修改，只在数据修改时加写锁不就行了。
 *  - 如果认为读锁是为了防止多线程读到的数据不一致，
 *    实际上即使加读锁，还是会存在有的线程读旧值，有的线程读新值。
 *  - 任何锁表面上是互斥，但本质是都是为了避免原子性问题（如果程序没有原子性问题，
 *    那只用volatile来避免可见性和有序性问题就可以了，效率更高）
 *    读锁自然也是为了避免原子性问题，避免同时读和写时，读到的数可能是写操作的中间状态。
 *    读锁是防止读到写操作的中间状态的值。
 *  - 写锁和写锁互斥的原因是避免同时写和写是，一个写操作会覆盖另一个写操作的结果，导致实际
 *    最后写成的结果是中间状态的值。
 *
 */
public class _04_ReadWriteLock {
}

// 使用读写锁实现一个通用的缓存工具类
class Cache<K,V>{
    private final Map<K, V> map = new HashMap<>();

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    // 读锁
    private final Lock r = rwl.readLock();

    // 写锁
    private final Lock w = rwl.writeLock();

    // 读缓存
    // 对于源头数据的数据量不大，就可以采用一次性加载的方式，
    // 全部数据在程序启动初始化时一次性加载到缓存中。
    public V get(K key){
        r.lock();
        try {
            return map.get(key);
        }finally {
            r.unlock();
        }
    }

    // 读缓存（懒加载）
    // 如果源头数据量非常大，那么就需要按需加载了，按需加载也叫懒加载，
    // 指的是只有当应用查询缓存，并且数据不在缓存里的时候，才触发加载源头相关数据进缓存的操作。
    public V getLazy(K key){
        V value = null;
        r.lock();
        try{
            value = map.get(key);
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
            // 再次验证，避免解锁后马上拿到写锁的线程，重复查询数据库更新缓存
            value = map.get(key);
            if (value == null){
                // 查询数据库
                value = (V) new Object();
                // 更新缓存
                map.put(key, value);
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
            map.put(key, value);
        }finally {
            w.unlock();
        }
    }

    // 读缓存（使用锁升级的方式，这是错误的写法）
    // 读写锁不支持锁升级，支持锁降级
    // 读锁还没有释放，此时获取写锁，会导致写锁永久等待，最终导致相关线程都被阻塞，永远也没有机会被唤醒。
    public V getLazyNotSupportUpdate(K key){
        V value = null;
        r.lock();
        try {
            value = map.get(key);
            if (value == null){
                // 读写锁不允许读锁升级为写锁
                w.lock();
                try{
                    value = map.get(key);
                    if (value == null){
                        // 查询数据库
                        value = (V) new Object();
                        // 更新缓存
                        map.put(key, value);
                    }
                }finally {
                    w.unlock();
                }
            }
        }finally {
            r.unlock();
        }
        return value;
    }

}

// 读写锁支持锁降级示例
abstract class CacheData{
    private Object data;
    private volatile boolean cacheValid;
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    public void processCacheData(){
        r.lock();

        // 缓存数据无效时
        if (!cacheValid){
            // 释放读锁，因为不允许读锁升级写锁
            r.unlock();
            w.lock();
            try {
                // 再次检查状态
                if (!cacheValid){
                    // 更新缓存数据
                    data = new Object();
                    cacheValid = true;
                }
                // 释放写锁前，将写锁降级为读锁。将并发限制降低（读读就不互斥）
                // 这种写锁降低为读锁的行为是允许的
                r.lock();
            }finally {
                w.unlock();
            }
        }

        // 缓存数据有效时
        try {
            use(data);
        }finally {
            r.unlock();
        }
    }

    public abstract void use(Object data);
}