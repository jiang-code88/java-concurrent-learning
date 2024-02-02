package com.learn._02_threadAndTools;

import java.util.concurrent.atomic.AtomicLong;


/**
 * 08 使用面向对象的思想写好并发程序
 *
 * - 面向对象 + 并发编程
 * - 封装：将属性和实现细节封装在对象内部，
 *        外部对象只能通过目标对象提供的公共方法来间接访问这些内部属性。
 *   - 对象属性：球场座位（共享变量）
 *   - 公共方法：球场入口（共享变量的访问路径）<= 增加并发访问策略
 * - 将共享变量作为对象属性封装在内部，对所有公共方法制定并发访问策略（实现线程安全类的常用方式）。
 * - 对于这些不会发生变化的共享变量，建议你用 final 关键字来修饰
 *   这样既能避免并发问题，也能很明了地表明你的设计意图，你已经考虑过这些共享变量的并发安全问题了。
 */

/**
 * 线程安全的计时器程序
 */
public class _08_Counter {
    // 共享变量
    private long value;

    // 公共方法（同步方法）
    public synchronized long get(){
        return value;
    }

    public synchronized long addOne(){
        return ++value;
    }
}

/**
 * 使用 AtomicLong 线程安全的原子类
 * 表示 upper 和 lower 成员变量，表示库存上限和下限，这样两个成员变量的 set 方法就不需要同步了。
 */
class SafeWM{ // 库存管理
    // 仓库库存上限
    private final AtomicLong upper = new AtomicLong(0);

    // 仓库库存下限
    private final AtomicLong lower = new AtomicLong(0);

    // 设置仓库库存上限
    public void setUpper(long value){
        upper.set(value);
    }

    // 设置仓库库存下限
    public void setLower(long value){
        lower.set(value);
    }
}

/**
 * 增加「库存的上限要小于库存下限」约束条件
 * 这里其实存在并发问题，问题在于存在竞态条件（当代码中出现 if 语句时，就应该立即意识到可能存在的竞态条件）
 * 最后结果就不符合「库存的上限要小于库存下限」约束条件
 */
class SafeWM_A {
    // 仓库库存上限
    private final AtomicLong upper = new AtomicLong(0);

    // 仓库库存下限
    private final AtomicLong lower = new AtomicLong(0);

    // 库存的上限要小于库存下限

    // 设置仓库库存上限
    public void setUpper(long value){
        // 检查参数合法性
        if (value < lower.get()){
            throw new IllegalArgumentException();
        }
        upper.set(value);
    }

    // 设置仓库库存下限
    public void setLower(long value){
        // 检查参数合法性
        if (value > upper.get()){
            throw new IllegalArgumentException();
        }
        lower.set(value);
    }
}

/**
 * 增加 synchronized(this) 同步锁，避免并发条件下出现问题
 */
class SafeWM_B {
    // 仓库库存上限
    private final AtomicLong upper = new AtomicLong(0);

    // 仓库库存下限
    private final AtomicLong lower = new AtomicLong(0);

    // 设置仓库库存上限
    public synchronized void setUpper(long value){
        // 检查参数合法性
        if (value < lower.get()){
            throw new IllegalArgumentException();
        }
        upper.set(value);
    }

    // 设置仓库库存下限
    public synchronized void setLower(long value){
        // 检查参数合法性
        if (value > upper.get()){
            throw new IllegalArgumentException();
        }
        lower.set(value);
    }
}
