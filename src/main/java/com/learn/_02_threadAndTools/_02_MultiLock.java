package com.learn._02_threadAndTools;

/**
 * 细粒度锁：使用不同的锁对受保护的资源进行精细化管理，提升性能
 *  - 账户余额使用一把锁，用户密码使用另一把锁
 *  - 细粒度锁使用的前提是保护的是没有关联关系的多个资源
 */
public class _02_MultiLock {
    // 账户余额
    private volatile long balance;
    // 保护账户余额的锁
    private final Object balLock = new Object();

    // 密码
    private String password;
    // 保护账户密码的锁
    private final Object pwLock = new Object();

    public _02_MultiLock(long balance, String password) {
        this.balance = balance;
        this.password = password;
    }

    // 取款
    public void withdraw(long amt){
        synchronized (balLock) {
            this.balance -= amt;
        }
    }

    // 查看余额
    public long getBalance(){
        synchronized (balLock) {
            return this.balance;
        }
    }

    // 更改密码
    public void updatePassword(String pw) {
        synchronized (pwLock){
            this.password = pw;
        }
    }

    // 查看密码
    public String getPassword(){
        synchronized (pwLock) {
            return this.password;
        }
    }

    // 并发测试程序是否存在线程安全问题
    public static void main(String[] args) throws InterruptedException {
        long count = 1000; // 账户总余额
        int unit = 10;    // 每个线程每次取款金额
        while (true) {
            _02_MultiLock account = new _02_MultiLock(count, "PAUL");
            System.out.println("初始金额：" + account.getBalance());

            // 启动多个线程, 将账户的钱取空
            long times = count / unit;
            for (int i = 0; i < times; i++) {
                new Thread(() -> {
                    account.withdraw(unit);
                }).start();
            }
            System.out.println("start withdraw threads end");

            // 主线程停一下, 保证所有子线程线程运行完
            Thread.sleep(5000);
            System.out.println("剩余金额：" + account.getBalance());
            System.out.println("-----------------");
        }
    }
}
