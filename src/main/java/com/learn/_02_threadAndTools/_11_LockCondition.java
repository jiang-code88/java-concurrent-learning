package com.learn._02_threadAndTools;

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
 */
public class _11_LockCondition {

    public static void main(String[] args) throws InterruptedException {
        // A -转账-> B
        // B -转账-> A
        // 会出现死锁的问题
        // transferAB();

        // A -转账-> B
        // B -转账-> A
        // 使用 tryLock() 非阻塞式加锁，避免死锁问题，但是会引入活锁问题
        // - 活锁：一直一直跑，一直一直拿不到锁，相对于死锁是一直等，同样是阻塞。
        // transferAB_tryLock();

        // A -转账-> B
        // B -转账-> A
        // 使用 tryLock() 非阻塞式加锁，避免死锁问题，但是会引入活锁问题
        // 引入随机时间解决活锁问题
        transferAB_tryLock_randomTime();
    }

    public static void transferAB() throws InterruptedException {
        // 账户初始总金额
        long count = 100;
        // 单次转账金额
        long unit = 20;

        Account_ReentrantLock accountA = new Account_ReentrantLock(count);
        Account_ReentrantLock accountB = new Account_ReentrantLock(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() -> accountA.transfer(accountB, unit));
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() -> accountB.transfer(accountA, unit));

        threadAB.start();
        threadBA.start();

        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance()
                + " " + "B=" + accountB.getBalance());
    }

    public static void transferAB_tryLock() throws InterruptedException {
        // 账户初始总金额
        long count = 100;
        // 单次转账金额
        long unit = 20;

        Account_ReentrantLock accountA = new Account_ReentrantLock(count);
        Account_ReentrantLock accountB = new Account_ReentrantLock(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() -> accountA.transfer_tryLock(accountB, unit));
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() -> accountB.transfer_tryLock(accountA, unit));

        threadAB.start();
        threadBA.start();

        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance()
                + " " + "B=" + accountB.getBalance());
    }

    public static void transferAB_tryLock_randomTime() throws InterruptedException {
        // 账户初始总金额
        long count = 100;
        // 单次转账金额
        long unit = 20;

        Account_ReentrantLock accountA = new Account_ReentrantLock(count);
        Account_ReentrantLock accountB = new Account_ReentrantLock(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() -> {
            try {
                accountA.transfer_tryLock_randomTime(accountB, unit);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() -> {
            try {
                accountB.transfer_tryLock_randomTime(accountA, unit);
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

class Account_ReentrantLock{

    private static Logger LOGGER = LoggerFactory.getLogger(_11_LockCondition.class);

    private long balance;
    private final Lock lock = new ReentrantLock();

    public Account_ReentrantLock(long balance){
        this.balance = balance;
    }

    public void transfer(Account_ReentrantLock target, long amt){
        // 锁定转出账户
        synchronized (this){
            // 在这里等一下让别人可以拿到 target 锁, 制造死锁
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 锁定转入账户
            synchronized (target) {
                this.balance -= amt;
                target.balance += amt;
            }
        }
    }

    public void transfer_tryLock(Account_ReentrantLock target, long amt){
        boolean flag = true;
        while (flag) {
            // 锁定转出账户
            LOGGER.info("加锁 this : {}", this.lock);
            if (this.lock.tryLock()) {
                try {
                    // 在这里等一下让别人可以拿到 target 锁, 制造活锁
                    Thread.sleep(100);
                    LOGGER.info("加锁 target : {}", target.lock);
                    if (target.lock.tryLock()) {
                        try {
                            this.balance -= amt;
                            target.balance += amt;
                            flag = false;
                            LOGGER.info("转账成功");
                        } finally {
                            LOGGER.info("解锁 target : {}", target.lock);
                            target.lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    LOGGER.info("解锁 this : {}", this.lock);
                    this.lock.unlock();
                }
            }

        }
    }

    public void transfer_tryLock_randomTime(Account_ReentrantLock target, long amt)
            throws InterruptedException {
        boolean success = false;
        while (!success) {
            // 锁定转出账户
            LOGGER.info("加锁 this : {}", this.lock);
            if (this.lock.tryLock(new Random().nextInt(100), TimeUnit.NANOSECONDS)){
                try {
                    // 在这里等一下让别人可以拿到 target 锁, 制造活锁
                    // Thread.sleep(100);
                    LOGGER.info("加锁 target : {}", target.lock);
                    if (target.lock.tryLock()) {
                        try {
                            this.balance -= amt;
                            target.balance += amt;
                            success = true;
                            LOGGER.info("转账成功");
                        } finally {
                            LOGGER.info("解锁 target : {}", target.lock);
                            target.lock.unlock();
                        }
                    }
                // } catch (InterruptedException e) {
                //     throw new RuntimeException(e);
                } finally {
                    LOGGER.info("解锁 this : {}", this.lock);
                    this.lock.unlock();
                }
            }
        }
    }

    public long getBalance() {
        return balance;
    }

}
