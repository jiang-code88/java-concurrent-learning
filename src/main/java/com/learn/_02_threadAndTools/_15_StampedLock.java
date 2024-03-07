package com.learn._02_threadAndTools;

import java.util.concurrent.locks.StampedLock;

/**
 * Java 读写锁 ReadWriteLock 的性能升级版 StampedLock
 *  - 在读多写少的场景中性能比 ReadWriteLock 更加优秀。
 *
 * StampedLock 与 ReadWriteLock 的对比：
 *  - StampedLock 提供「写锁、悲观读锁、乐观读」。
 *  - StampedLock 的写锁、悲观读锁的语义和 ReadWriteLock 的写锁、读锁的语义类似，
 *    均为允许多个线程同时获取悲观读锁，但是只允许一个线程获取写锁。
 *  - 不同之处是 StampedLock 里的写锁和悲观读锁加锁成功之后，都会返回一个 stamp；
 *    然后解锁的时候，需要传入这个 stamp
 *
 * StampedLock 的优势之处：
 *  - ReadWriteLock 支持多个线程同时读，但是当多个线程同时读的时候，所有的写操作会被阻塞；
 *    而 StampedLock 提供的乐观读，是允许一个线程获取写锁的，也就是说不是所有的写操作都被阻塞。
 *  - 乐观读这个操作是无锁的，所以相比较 ReadWriteLock 的读锁，乐观读的性能更好一些。
 * StampedLock 使用注意事项：
 *  - StampedLock 不支持重入
 *  - 使用 StampedLock 一定不要调用中断操作，如果需要支持中断功能，
 *    一定使用可中断的悲观读锁 readLockInterruptibly() 和写锁 writeLockInterruptibly()
 *  - StampedLock 支持锁的降级（通过 tryConvertToReadLock() 方法实现）
 *    和升级（通过 tryConvertToWriteLock() 方法实现）
 */
public class _15_StampedLock {

    // 1 StampedLock 的使用示例
    public static void main(String[] args) {
        final StampedLock sl = new StampedLock();

        // 获取/释放悲观读锁的示意代码
        long rStamp = sl.readLock();
        try {
            // 省略互斥操作
        }finally {
            sl.unlockRead(rStamp);
        }


        // 获取/释放写锁的示意代码
        long wStamp = sl.writeLock();
        try {
            // 省略互斥操作
        }finally {
            sl.unlockWrite(wStamp);
        }
    }

    // 3 StampedLock 实践中的使用模版
    public static void StampedLockUseTemplate(){
        final StampedLock sl = new StampedLock();

        // StampedLock 读模板：
        // 乐观读
        long stamp = sl.tryOptimisticRead();
        // 读入方法局部变量......
        // 校验stamp
        if (!sl.validate(stamp)){
            // 升级为悲观读锁
            stamp = sl.readLock();
            try {
                // 读入方法局部变量 .....
            } finally {
                //释放悲观读锁
                sl.unlockRead(stamp);
            }
        }//使用方法局部变量执行业务操作......


        // StampedLock 写模板：
        long wStamp = sl.writeLock();
        try {
            // 写共享变量
            // ......
        }finally {
            sl.unlockWrite(wStamp);
        }
    }
}

// 2 场景：计算坐标（x，y）到原点的距离
// StampedLock 乐观读的用法示例
class Point{
    private int x, y;
    private final StampedLock sl = new StampedLock();
    public int distanceFromOrigin(){
        // 乐观读
        // - 相当于乐观的觉得现在没有写操作，不可能读到写操作的中间状态，
        //   所以不需要读写互斥保护（省略加读锁操作，所以比 ReadWriteLock 性能好）。
        // - 相当于这里打个版本号标记一下，然后实际操作变量时，需要判断一下版本号有没有变
        //   也就是判断一下，过去期间有没有人获取过写锁修改过（获取过写锁版本号就变）
        //   如果变了，就说明期间数据被改过了，需要加读锁和可能再有的写操作互斥住（防止读到中间状态），然后再读取一次最新数据。
        //   如果没变，就说明期间数据没被改过，最多也是其他人来读过，没问题放心使用。
        long stamp = sl.tryOptimisticRead();
        // 读取局部变量
        // 读的过程数据可能被修改
        int curX = x;
        int curY = y;
        // 判断执行读操作期间，
        // 是否存在写操作，如果存在，则 sl.validate 返回 false
        if (!sl.validate(stamp)){
            // 升级为悲观读锁
            // 执行乐观读操作的期间，存在写操作，会把乐观读升级为悲观读锁，
            // 否则你就需要在一个循环里反复执行乐观读，直到执行乐观读操作的期间没有写操作（只有这样才能保证 x 和 y 的正确性和一致性），
            // 而循环读会浪费大量的 CPU。升级为悲观读锁，代码简练且不易出错，建议在具体实践中使用。
            stamp = sl.readLock();
            try {
                // 重新读取一次最新的数据
                curX = x;
                curY = y;
            }finally {
                // 释放悲观读锁
                sl.unlockRead(stamp);
            }
        }
        // 使用数据计算坐标到原点的距离（勾股定理）
        return (int) Math.sqrt(curX * curX + curY * curY);
    }
}
