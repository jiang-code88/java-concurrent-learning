package com.learn._02_concurrentUtils;

import com.learn.common.CommTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JUC 包中的管程 Lock 和 Condition 组件
 *  - Java SDK 并发包通过 Lock 和 Condition 两个接口来实现管程，
 *    其中 Lock 用于解决互斥问题，Condition 用于解决同步问题。
 *  - Lock 有别于 synchronized 隐式锁的区别是：能够响应中断、支持超时和非阻塞地获取锁
 *
 * 用锁的最佳实践：
 *  1.永远只在更新对象的成员变量时加锁
 *  2.永远只在访问可变的成员变量时加锁
 *  3.永远不在调用其他对象的方法时加锁
 *
 *  - 减少锁的持有时间
 *  - 减小锁的粒度
 */
public class _01_LockCondition_1 {

    public static void main(String[] args) throws InterruptedException {
        // A -转账-> B
        // B -转账-> A
        // 会出现死锁的问题
        // transferAB();

        // A -转账-> B
        // B -转账-> A
        // 使用 tryLock() 非阻塞式加锁，避免死锁问题，但是会引入活锁问题
        // - 活锁：一直一直跑，一直一直拿不到锁，相对于死锁是一直等，同样是阻塞。
        // transferABTryLock();

        // A -转账-> B
        // B -转账-> A
        // 使用 tryLock() 非阻塞式加锁，避免死锁问题，但是会引入活锁问题
        // 引入线程随机睡眠时间解决活锁问题
        transferABTryLockRandomTime();
    }

    public static void transferAB() throws InterruptedException {
        // 账户初始总金额
        long count = 100;
        // 单次转账金额
        long unit = 20;

        ReentrantLockAccount accountA = new ReentrantLockAccount(count);
        ReentrantLockAccount accountB = new ReentrantLockAccount(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(
                () -> accountA.transfer(accountB, unit)
        );
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(
                () -> accountB.transfer(accountA, unit)
        );

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance()
                + " " + "B=" + accountB.getBalance());
    }

    public static void transferABTryLock() throws InterruptedException {
        // 账户初始总金额
        long count = 100;
        // 单次转账金额
        long unit = 20;

        ReentrantLockAccount accountA = new ReentrantLockAccount(count);
        ReentrantLockAccount accountB = new ReentrantLockAccount(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(
                () -> accountA.transferTryLock(accountB, unit)
        );
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(
                () -> accountB.transferTryLock(accountA, unit)
        );

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance()
                + " " + "B=" + accountB.getBalance());
    }

    public static void transferABTryLockRandomTime() throws InterruptedException {
        // 账户初始总金额
        long count = 100;
        // 单次转账金额
        long unit = 20;

        ReentrantLockAccount accountA = new ReentrantLockAccount(count);
        ReentrantLockAccount accountB = new ReentrantLockAccount(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() -> {
            try {
                accountA.transferTryLockRandomTime(accountB, unit);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() -> {
            try {
                accountB.transferTryLockRandomTime(accountA, unit);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance()
                + " " + "B=" + accountB.getBalance());
    }
}

class ReentrantLockAccount{
    private static Logger LOGGER = LoggerFactory.getLogger(_01_LockCondition_1.class);
    private long balance;
    private final Lock lock = new ReentrantLock();

    public ReentrantLockAccount(long balance){
        this.balance = balance;
    }

    public void transfer(ReentrantLockAccount target, long amt){
        // 锁定转出账户
        lock.lock();
        LOGGER.info("加锁 this : {}", this.lock);
        try {
            // 在这里等一下让别人可以拿到 target 锁, 制造死锁
            CommTools.sleep(100, TimeUnit.MILLISECONDS);
            // 锁定转入账户
            target.lock.lock();
            LOGGER.info("加锁 target : {}", target.lock);
            try {
                this.balance -= amt;
                target.balance += amt;
            }finally {
                target.lock.unlock();
                LOGGER.info("解锁 target : {}", target.lock);
            }
        }finally {
            lock.unlock();
            LOGGER.info("解锁 this : {}", this.lock);
        }
    }

    public void transferTryLock(ReentrantLockAccount target, long amt){
        boolean success = true;
        while (success) {
            // 锁定转出账户
            LOGGER.info("尝试加锁 this : {}", this.lock);
            if (this.lock.tryLock()) {
                try {
                    // 在这里等一下让别人可以拿到 target 锁, 制造活锁
                    CommTools.sleep(100, TimeUnit.MILLISECONDS);
                    // 锁定转入账户
                    LOGGER.info("尝试加锁 target : {}", target.lock);
                    if (target.lock.tryLock()) {
                        try {
                            this.balance -= amt;
                            target.balance += amt;
                            success = false;
                            LOGGER.info("转账成功");
                        } finally {
                            target.lock.unlock();
                            LOGGER.info("解锁 target : {}", target.lock);
                        }
                    }
                } finally {
                    this.lock.unlock();
                    LOGGER.info("解锁 this : {}", this.lock);
                }
            }

        }
    }

    public void transferTryLockRandomTime(ReentrantLockAccount target, long amt)
            throws InterruptedException {
        boolean success = false;
        while (!success) {
            // 锁定转出账户
            LOGGER.info("尝试加锁 this : {}", this.lock);
            if (this.lock.tryLock()){
                try {
                    // 在这里等一下让别人可以拿到 target 锁, 制造活锁
                    CommTools.sleep(100, TimeUnit.MILLISECONDS);
                    // 锁定转入账户
                    LOGGER.info("尝试加锁 target : {}", target.lock);
                    if (target.lock.tryLock()) {
                        try {
                            this.balance -= amt;
                            target.balance += amt;
                            success = true;
                            LOGGER.info("转账成功");
                        } finally {
                            target.lock.unlock();
                            LOGGER.info("解锁 target : {}", target.lock);
                        }
                    }
                } finally {
                    this.lock.unlock();
                    LOGGER.info("解锁 this : {}", this.lock);
                }
            } // if
            // 在 while 循环体结束前，增加线程随机睡眠时间，解决活锁问题：
            // 如果两个线程恰巧发生同步运行将陷入活锁，让线程随机睡眠一段时间可以错开同步运行，解决活锁死循环问题。
            CommTools.sleep(new Random().nextInt(100), TimeUnit.NANOSECONDS);
        } // while
    }

    public long getBalance() {
        return balance;
    }

}
