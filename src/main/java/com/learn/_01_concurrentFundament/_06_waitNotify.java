package com.learn._01_concurrentFundament;


import com.learn.common.Account;
import com.learn.common.CommTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用等待-通知机制（synchronize、wait、notify）改造持续循环，持续占用 CPU 的情况
 * - 置入等待调度，优化 CPU 的占用性能
 */
public class _06_waitNotify {

    public static void main(String[] args) {
        // 账户原始金额
        long count = 7000000;
        // 账户单次转账金额
        long unit = 100;

        while (true) {
            System.out.println("==================================");
            // 初始化 A、B、C 账户
            Account accountA = new WaitNotifyAccount(count);
            Account accountB = new WaitNotifyAccount(count);
            Account accountC = new WaitNotifyAccount(count);

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
            CommTools.sleep(6, TimeUnit.SECONDS);

            // 打印结果
            System.out.println("result: A=" + accountA.getBalance() + " " +
                    "B=" + accountB.getBalance() + " " + "C=" + accountC.getBalance());
            System.out.println("==================================\n");
        }
    }

}

class WaitNotifyAccount extends Account {
    static class Allocator {
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
            this.als.remove(from);
            this.als.remove(to);
            // notify() 是会随机地通知等待队列中的一个线程，
            // notifyAll() 是会通知等待队列中的所有线程。
            // 实际上 notify() 存在某些线程永远不会被通知到的风险，所以不建议使用。
            notifyAll();
        }
    }

    // 保证单例的 Allocator 对象
    public static final Allocator ALLOCATOR = new Allocator();

    public WaitNotifyAccount(long balance) {
        super(balance);
    }

    @Override
    public void transfer(Account target, long amt){
        // 一次性获取所有资源, 不断的尝试获取, 成功获取才操作/不成功则阻塞，等待条件满足被唤醒
        ALLOCATOR.apply(this, target);
        try {
            // 锁定转出账户
            synchronized (this){
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