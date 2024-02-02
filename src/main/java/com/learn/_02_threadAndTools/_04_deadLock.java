package com.learn._02_threadAndTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 细粒度锁改造 Account.class 类锁对象，提高性能
 * 但是引入了死锁的问题。使用有序加锁和一次性申请全部锁的方式，避免死锁发生。
 */
public class _04_deadLock {

    public static void main(String[] args) throws InterruptedException {
        // A --转账--> B
        // B --转账--> C
        // 使用细粒度锁改造 Account.class 类对象锁，提高并行度优化性能
        // transfer_A_B_C();

        // A --转账--> B
        // B --转账--> A
        // 制造死锁的场景
        // deadLock();

        // A --转账--> B
        // B --转账--> A
        // 对互斥资源进行排序，按序加锁，避免死锁发生
        // transfer_orderly();

        // A --转账--> B
        // B --转账--> A
        // 一次性申请 A 锁和 B 锁后才访问互斥资源，避免死锁发生
        // transfer_apply_all();
    }

    public static void transfer_A_B_C() throws InterruptedException {
        // 账户原始金额
        long count = 7000000;
        // 账户单次转账金额
        long unit = 100;

        while (true) {
            // A 账户
            AAccount accountA = new AAccount(count);
            // B 账户
            AAccount accountB = new AAccount(count);
            // C 账户
            AAccount accountC = new AAccount(count);

            long times = count / unit;
            HashSet<Thread> threadHashSet = new HashSet<>();
            while (times-- > 0) {
                Thread threadA = new Thread(() -> accountA.transfer_apply_all_lock(accountB, unit));
                threadHashSet.add(threadA);
                Thread threadB = new Thread(() -> accountB.transfer_apply_all_lock(accountC, unit));
                threadHashSet.add(threadB);
            }

            for (Thread thread : threadHashSet) {
                thread.start();
            }

            Thread.sleep(5000);

            System.out.println("result: A=" + accountA.getBalance() + " " +
                    "B=" + accountB.getBalance() + " " + "C=" + accountC.getBalance());
        }
    }

    public static void deadLock() throws InterruptedException {
        // 账户初始总金额
        long count = 1000;
        // 单次转账金额
        long unit = 200;

        AAccount accountA = new AAccount(count);
        AAccount accountB = new AAccount(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() -> accountA.transfer_deadLock(accountB, unit));
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() -> accountB.transfer_deadLock(accountA, unit));

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance() + " " +
                "B=" + accountB.getBalance());
    }

    public static void transfer_orderly() throws InterruptedException {
        // 账户初始总金额
        long count = 1000;
        // 单次转账金额
        long unit = 200;

        AAccount accountA = new AAccount(1, count);
        AAccount accountB = new AAccount(2, count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() -> accountA.transfer_orderly_lock(accountB, unit));
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() -> accountB.transfer_orderly_lock(accountA, unit));

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance() + " " +
                "B=" + accountB.getBalance());
    }

    public static void transfer_apply_all() throws InterruptedException {
        // 账户初始总金额
        long count = 1000;
        // 单次转账金额
        long unit = 200;

        AAccount accountA = new AAccount(count);
        AAccount accountB = new AAccount(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() -> accountA.transfer_apply_all_lock(accountB, unit));
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() -> accountB.transfer_apply_all_lock(accountA, unit));

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance() + " " +
                "B=" + accountB.getBalance());
    }
}

class AAccount{
    private long balance;
    private int id;

    public AAccount(long balance){
        this.balance = balance;
    }

    public AAccount(int id, long balance){
        this.id = id;
        this.balance = balance;
    }

    public void transfer(AAccount target, long amt){
        // 锁定转出账户
        synchronized (this){
            // 锁定转入账户
            synchronized (target) {
                this.balance -= amt;
                target.balance += amt;
            }
        }
    }

    public void transfer_deadLock(AAccount target, long amt){
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

    public void transfer_orderly_lock(AAccount target, long amt){
        Object minLock = this;
        Object maxLock = target;
        if (this.id > target.id){
            minLock = target;
            maxLock = this;
        }
        // 锁定转出账户
        synchronized (minLock){
            // 在这里等一下让别人可以拿到 target 锁, 制造死锁
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // 锁定转入账户
            synchronized (maxLock) {
                this.balance -= amt;
                target.balance += amt;
            }
        }
    }

    public void transfer_apply_all_lock(AAccount target, long amt){
        // 一次性获取所有资源, 不断的尝试获取, 成功获取才操作/不成功则重新获取
        while (!Allocator.ALLOCATOR.apply(this, target));
        try {
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
        }finally {
            Allocator.ALLOCATOR.free(this, target);
        }
    }

    public long getBalance() {
        return balance;
    }
}

class Allocator {
    // 保证单例的 Allocator 对象
    public static final Allocator ALLOCATOR = new Allocator();

    // 资源列表
    private List<Object> als = new ArrayList<>();

    // 一次性申请全部资源
    public synchronized boolean apply(Object from, Object to){
        if (als.contains(from) || als.contains(to)){
            return false;
        }else {
            als.add(from);
            als.add(to);
            return true;
        }
    }

    // 归还资源
    public synchronized void free(Object from, Object to){
        // this.als.clear();
        // 文章的实现是逐个资源从列表中移除, 这是有好处的
        // 只释放自己一次性申请到的全部资源, 不会影响到别人一次性申请到的资源
        // [A,B]    ===>    [A,B,C,D]
        // 释放 [A,B] 资源时如果把 [C,D] 也释放了, 就可能别人来加锁造成死锁
        this.als.remove(from);
        this.als.remove(to);
    }
}
