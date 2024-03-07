package com.learn.common;

public abstract class Account{
    private long balance;

    public Account(long balance) {
        this.balance = balance;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }


    public abstract void transfer(Account target, long amt);
}
