package com.learn._01_concurrentFundament;

import com.learn.common.Account;
import com.learn.common.CommTools;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * 使用类对象锁，保护有关联关系的多个资源
 */
class BankAccount extends Account {
    public BankAccount(long balance){
        super(balance);
    }

    public void transfer(Account target, long amt){
        // 用 Account.class 作为共享的锁。Account.class 是所有 Account 对象共享的，
        // 而且这个对象是 Java 虚拟机在加载 Account 类的时候创建的，所以我们不用担心它的唯一性。
        synchronized (Account.class) {
            this.setBalance(this.getBalance() - amt);
            target.setBalance(target.getBalance() + amt);
        }
    }
}

public class _04_ClassLock {
    public static void main(String[] args){
        // 场景: A 账户 --转账--> B 账户 --转账--> C 账户
        // 结果: A 账户中的钱全部转账到 C 账户变成 0, B 账户中钱不变，C 账户中钱变成两倍

        // 账户原始金额
        long count = 7000000;
        // 账户单次转账金额
        long unit = 100;

        while (true) {
            System.out.println("==================================");
            // 初始化 A、B、C 账户
            BankAccount accountA = new BankAccount(count);
            BankAccount accountB = new BankAccount(count);
            BankAccount accountC = new BankAccount(count);

            // 执行 A -转账-> B 和 B -转账-> C 的线程
            long threadNum = count / unit;
            HashSet<Thread> threadHashSet = new HashSet<>();
            while (threadNum-- > 0) {
                threadHashSet.add(new Thread(() -> accountA.transfer(accountB, unit)));
                threadHashSet.add(new Thread(() -> accountB.transfer(accountC, unit)));
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
}

