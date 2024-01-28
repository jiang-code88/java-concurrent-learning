package com.learn._02_synchronized;

/**
 * Java 读写锁 ReadWriteLock 的升级版 StampedLock
 *  - StampedLock 提供 写锁、悲观读锁和乐观读（乐观读是无锁操作）
 *  - StampedLock 里的写锁和悲观读锁加锁成功之后，都会返回一个 stamp；然后解锁的时候，需要传入这个 stamp
 *  - ReadWriteLock 支持多个线程同时读，但是当多个线程同时读的时候，所有的写操作会被阻塞；
 *    而 StampedLock 提供的乐观读，是允许一个线程获取写锁的，也就是说不是所有的写操作都被阻塞。
 *
 *  - StampedLock 不支持重入
 *
 *  - StampedLock 支持锁的降级（通过 tryConvertToReadLock() 方法实现）和升级（通过 tryConvertToWriteLock() 方法实现）
 */
public class _15_StampedLock {
}
