package com.learn._02_threadAndTools;

import java.util.HashSet;

/**
 * 使用类对象锁，保护有关联关系的多个资源
 */
public class _03_ClassLock {
    public static void main(String[] args) throws InterruptedException {
        // 场景: A --转账--> B --转账--> C
        // 结果: A 中的钱全部转账到 C 变成 0, B 中钱不变，C 中钱变成 14000000

        // 账户原始金额
        long count = 7000000;
        // 账户单次转账金额
        long unit = 100;

        while (true) {
            // A 账户
            Account accountA = new Account(count);
            // B 账户
            Account accountB = new Account(count);
            // C 账户
            Account accountC = new Account(count);

            // 开启两个线程分别做 A --> B 和 B --> C
            // 虽然操作 B 资源的两个线程加了锁, 但是加的是各自的 this 锁, 不是同一把锁是两把锁,
            // 所以两个线程同时访问 B 资源, 没有互斥访问会出错了, 会在小于 count 和大于 count 的范围内波动
            // 解决方案: 更换为加一把对于 Account 对象唯一的 Account.class 类 Class 对象锁
            long times = count / unit;
            HashSet<Thread> threadHashSet = new HashSet<>();
            while (times-- > 0) {
                Thread threadA = new Thread(() -> accountA.transfer(accountB, unit));
                threadHashSet.add(threadA);
                Thread threadB = new Thread(() -> accountB.transfer(accountC, unit));
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
}

class Account{
    private long balance;

    public Account(long balance){
        this.balance = balance;
    }

    public void transfer(Account target, long amt){
        // 用 Account.class 作为共享的锁。Account.class 是所有 Account 对象共享的，
        // 而且这个对象是 Java 虚拟机在加载 Account 类的时候创建的，所以我们不用担心它的唯一性。
        synchronized (Account.class) {
            this.balance -= amt;
            target.balance += amt;
        }
    }

    public long getBalance() {
        return balance;
    }
}
