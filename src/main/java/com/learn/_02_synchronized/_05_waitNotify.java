package com.learn._02_synchronized;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 使用等待-通知机制（synchronize、wait、notify）改造持续循环，持续占用 CPU 的情况
 * - 置入等待调度，优化 CPU 的占用性能
 */
public class _05_waitNotify {

    public static void main(String[] args) throws InterruptedException {
        transfer();
    }

    public static void transfer() throws InterruptedException {
        // 账户原始金额
        long count = 700000;
        // 账户单次转账金额
        long unit = 100;

        while (true) {
            // A 账户
            AAAccount accountA = new AAAccount(count);
            // B 账户
            AAAccount accountB = new AAAccount(count);
            // C 账户
            AAAccount accountC = new AAAccount(count);

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

            Thread.sleep(6000);

            System.out.println("result: A=" + accountA.getBalance() + " " +
                    "B=" + accountB.getBalance() + " " + "C=" + accountC.getBalance());
        }
    }
}

class AAAccount{
    private long balance;

    public AAAccount(long balance){
        this.balance = balance;
    }

    public void transfer_apply_all_lock(AAAccount target, long amt){
        // 一次性获取所有资源, 不断的尝试获取, 成功获取才操作/不成功则阻塞，等待条件满足被唤醒
        AAllocator.ALLOCATOR.apply(this, target);
        try {
            // 锁定转出账户
            synchronized (this){
                // 锁定转入账户
                synchronized (target) {
                    this.balance -= amt;
                    target.balance += amt;
                }
            }
        }finally {
            AAllocator.ALLOCATOR.free(this, target);
        }
    }

    public long getBalance() {
        return balance;
    }
}

class AAllocator {
    // 保证单例的 Allocator 对象
    public static final AAllocator ALLOCATOR = new AAllocator();

    // 资源列表
    private List<Object> als = new ArrayList<>();

    // 一次性申请全部资源
    public synchronized void apply(Object from, Object to){
        while (als.contains(from) || als.contains(to)){
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        als.add(from);
        als.add(to);
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
        // notify() 是会随机地通知等待队列中的一个线程，而 notifyAll() 会通知等待队列中的所有线程。
        // 实际上 notify() 存在某些线程永远不会被通知到的风险。
        notifyAll();
    }
}