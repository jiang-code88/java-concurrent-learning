package com.learn._01_concurrentFundament;

import com.learn.common.Account;
import com.learn.common.CommTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 细粒度锁改造 Account.class 类锁对象，提高性能
 * 但是会引入了死锁的问题。使用有序加锁或一次性申请全部锁的方式，避免死锁发生。
 */
public class _05_deadLock {

    public static void main(String[] args) throws InterruptedException {
        // A --转账--> B
        // B --转账--> C
        // 使用细粒度锁改造 Account.class 类对象锁，提高并行度优化性能
        // transferABC();

        // A --转账--> B
        // B --转账--> A
        // 制造死锁的场景
        // transferAB();

        // A --转账--> B
        // B --转账--> A
        // 对互斥资源进行排序，按序加锁，避免死锁发生
        // transferAB_orderly();

        // A --转账--> B
        // B --转账--> A
        // 一次性申请 A 锁和 B 锁后才访问互斥资源，避免死锁发生
        // transferAB_applyAll();
    }

    public static void transferABC() throws InterruptedException {
        // 账户原始金额
        long count = 7000000;
        // 账户单次转账金额
        long unit = 100;

        while (true) {
            System.out.println("==================================");
            // 初始化 A、B、C 账户
            MultiLockAccount accountA = new MultiLockAccount(count);
            MultiLockAccount accountB = new MultiLockAccount(count);
            MultiLockAccount accountC = new MultiLockAccount(count);

            // 执行 A -转账-> B 和 B -转账-> C 的线程
            long threadNum = count / unit;
            HashSet<Thread> threadHashSet = new HashSet<>();
            while (threadNum-- > 0) {
                threadHashSet.add(new Thread(
                        () -> accountA.transfer(accountB, unit)));
                threadHashSet.add(new Thread(
                        () -> accountB.transfer(accountC, unit)));
            }

            // 启动所有子线程
            for (Thread thread : threadHashSet) {
                thread.start();
            }

            // 等待子线程执行完毕
            CommTools.sleep(5, TimeUnit.SECONDS);

            // 打印结果
            System.out.println("result: A=" + accountA.getBalance() + " " +
                    "B=" + accountB.getBalance() + " " + "C=" + accountC.getBalance());
            System.out.println("==================================\n");
        }
    }

    public static void transferAB() throws InterruptedException {
        // 账户初始总金额
        long count = 1000;
        // 单次转账金额
        long unit = 200;

        Account accountA = new DeadLockAccount(count);
        Account accountB = new DeadLockAccount(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() ->
                accountA.transfer(accountB, unit));
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() ->
                accountB.transfer(accountA, unit));

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance() + " " +
                "B=" + accountB.getBalance());
    }

    public static void transferAB_orderly() throws InterruptedException {
        // 账户初始总金额
        long count = 1000;
        // 单次转账金额
        long unit = 200;

        Account accountA = new OrderlyLockAccount(count, 1);
        Account accountB = new OrderlyLockAccount(count, 2);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() ->
                accountA.transfer(accountB, unit));
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() ->
                accountB.transfer(accountA, unit));

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance() + " " +
                "B=" + accountB.getBalance());
    }

    public static void transferAB_applyAll() throws InterruptedException {
        // 账户初始总金额
        long count = 1000;
        // 单次转账金额
        long unit = 200;

        Account accountA = new AllApplyAccount(count);
        Account accountB = new AllApplyAccount(count);

        // 先拿 A 锁，再拿 B 锁
        Thread threadAB = new Thread(() ->
                accountA.transfer(accountB, unit));
        // 先拿 B 锁，再拿 A 锁
        Thread threadBA = new Thread(() ->
                accountB.transfer(accountA, unit));

        threadAB.start();
        threadBA.start();
        threadAB.join();
        threadBA.join();

        System.out.println("result: A=" + accountA.getBalance() + " " +
                "B=" + accountB.getBalance());
    }
}

class MultiLockAccount extends Account{

    public MultiLockAccount(long balance) {
        super(balance);
    }

    @Override
    public void transfer(Account target, long amt) {
        // 锁定转出账户
        synchronized (this){
            // 锁定转入账户
            synchronized (target) {
                this.setBalance(this.getBalance() - amt);
                target.setBalance(target.getBalance() + amt);
            }
        }
    }
}

class DeadLockAccount extends Account{

    public DeadLockAccount(long balance) {
        super(balance);
    }

    @Override
    public void transfer(Account target, long amt) {
        // 锁定转出账户
        synchronized (this){
            // 在这里等一下让别人可以拿到 target 锁, 制造死锁
            CommTools.sleep(100, TimeUnit.MILLISECONDS);
            // 锁定转入账户
            synchronized (target) {
                this.setBalance(this.getBalance() - amt);
                target.setBalance(target.getBalance() + amt);
            }
        }
    }
}

class OrderlyLockAccount extends Account{
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public OrderlyLockAccount(long balance, int id) {
        super(balance);
        this.id = id;
    }

    @Override
    public void transfer(Account target, long amt) {
        transferOrderly((OrderlyLockAccount) target, amt);
    }

    private void transferOrderly(OrderlyLockAccount target, long amt){
        Object minLock = this;
        Object maxLock = target;
        if (this.getId() > target.getId()){
            minLock = target;
            maxLock = this;
        }
        // 锁定转出账户
        synchronized (minLock){
            // 在这里等一下让别人可以拿到 target 锁, 制造死锁
            CommTools.sleep(100, TimeUnit.MILLISECONDS);
            // 锁定转入账户
            synchronized (maxLock) {
                this.setBalance(this.getBalance() - amt);
                target.setBalance(target.getBalance() + amt);
            }
        }
    }
}

class AllApplyAccount extends Account{
     static class Allocator {
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

    // 保证单例的 Allocator 对象
    public static final Allocator ALLOCATOR = new Allocator();

    public AllApplyAccount(long balance) {
        super(balance);
    }

    @Override
    public void transfer(Account target, long amt) {
        // 一次性获取所有资源, 不断的尝试获取, 成功获取才操作/不成功则重新获取
        while (!ALLOCATOR.apply(this, target));
        try {
            // 锁定转出账户
            synchronized (this){
                // 在这里等一下让别人可以拿到 target 锁, 制造死锁
                CommTools.sleep(100, TimeUnit.MILLISECONDS);
                // 锁定转入账户
                synchronized (target) {
                    this.setBalance(this.getBalance() - amt);
                    target.setBalance(target.getBalance() + amt);
                }
            }
        }finally {
            ALLOCATOR.free(this, target);
        }
    }
}
