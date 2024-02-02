package com.learn._02_threadAndTools;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 原子类并发工具：使用无锁方案保障原子性，相比于互斥锁方案，最大的好处是「性能」的提高
 *  - 互斥锁方案的缺点：加锁、解锁操作本身就消耗性能，同时拿不到锁的线程还会进入阻塞状态，
 *    进而触发线程切换，线程切换对性能的消耗也很大。
 *  - 无锁方案的实现原理：CAS 指令 + 自旋 + volatile 共享变量
 * 原子类概述：
 *  1）原子化的基本数据类型
 *   - AtomicBoolean
 *   - AtomicInteger
 *   - AtomicLong
 *  2）原子化的对象引用类型
 *   - AtomicReference
 *   - AtomicStampedReference
 *   - AtomicMarkableReference
 *   AtomicStampedReference 和 AtomicMarkableReference 可以解决 ABA 问题。
 *   AtomicStampedReference 解决原理是每次 CAS 操作都伴随一个版本号递增变化，
 *   那么不断 A 怎么从 B 又变化为 A，版本号都是递增，都可以通过版本号记录到其变化。
 *   AtomicMarkableReference 的实现机制则更简单，将版本号简化成了一个 Boolean 值。
 *  3）原子化数组
 *   - AtomicIntegerArray
 *   - AtomicLongArray
 *   - AtomicReferenceArray
 *  4）原子化对象属性更新器
 *   - AtomicIntegerFieldUpdater
 *   - AtomicLongFieldUpdater
 *   - AtomicReferenceFieldUpdater
 *   对象属性必须是 volatile 类型的，只有这样才能保证可见性
 *  5）原子化的累加器
 *   - DoubleAccumulator
 *   - DoubleAdder
 *   - LongAccumulator
 *   - LongAdder
 *   这四个类仅仅用来执行累加操作，相比原子化的基本数据类型，速度更快，
 *   但是不支持 compareAndSet() 方法。
 * 总结：
 *  - 无锁方案比互斥锁方案，性能好很多，不会出现死锁问题（但是存在活锁和饥饿问题，因为自旋会反复重试）
 *  - Java 原子类大部分实现了 compareAndSet() 方法，基于该方法我们可以构建，自己的无锁数据结构（但是建议用内置的）。
 *  - 原子类的方法是针对单个共享变量的，如果是解决多个共享变量的原子性问题，建议使用互斥锁的方案。
 */
public class _18_Atomic {
    // 1 原子类的使用示例
    private AtomicLong Count = new AtomicLong(0);
    public void add10K(){
        long idx = 0;
        while (idx++ < 1000){
            Count.getAndIncrement();
        }
    }

    // 2 CAS(Compare And Swap) 指令
    //  - 共享变量内存地址 A
    //  - 用于比较的期望值 B
    //  - 共享变量的新值   C
    // CAS 指令本身是能够保证原子性的（由操作系统提供的指令原子性能力）
    private volatile long count = 0;
    // 模拟 CAS 指令操作
    public synchronized long cas(long expectValue, long newValue){
        // 读取 count 的值
        long currentValue = count;
        // 比较目前 count 的值是否等于期望值
        if (currentValue == expectValue){
            count = newValue;
        }
        // 返回写入前的值（等于期望就是改成功，不等于期望就是没改成功）
        return currentValue;
    }
    // 增加自旋（循环尝试）达到无锁保证线程安全修改共享变量值
    // 原子性的 count += 1 操作
    public void addOne(){
        long oldValue;
        long newValue;
        do {
            // 读取内存中的值
            oldValue = count;
            // 执行该语句后，执行 cas 操作前，共享变量 count 被改变
            newValue = oldValue + 1;
            // 自旋重新读 count 最新的值来计算 newValue 并尝试再次更新，直到成功。
        }while (oldValue != cas(oldValue, newValue));
    }

    // 3 CAS 方案中存在的问题是：ABA 问题，也就是期望修改的是 A，然后中途更新为 B，又更新为 A。
    // 如果此 A 非彼 A，就会出现问题，大多数场景都不需要关心 ABA 问题。
    //  - 原子化的更新对象需要关注 ABA 问题，
    //    因为两个 A 虽然相等，但是第二个 A 的内部属性可能已经发生变化了。
    //    所以在使用 CAS 方案的时候，一定要先 check 一下是不是完全相等的。

    // 4 JDK 中 CAS 使用的经典范例
    public void method(){
        // do {
        //     // 获取当前值
        //     oldV = xxxx;
        //     // 根据当前值计算新值
        //     newV = ...oldV...;
        // }while(!compareAndSet(oldV,newV);
        // // compareAndSet() 里面如果更新成功，则会返回 true，否则返回 false。
    }

}

/**
 * 原子化的对象引用类型使用示例
 */
class SafeWM_C { // 原子化的对象引用类型

    // 仓库库存范围
    class WMRange{
        final int upper;
        final int lower;
        WMRange(int upper,int lower){
            this.upper = upper;
            this.lower = lower;
        }
    }

    // 仓库库存初始值
    final AtomicReference<WMRange> rf =
            new AtomicReference<>(new WMRange(0,0));

    // 设置库存上限
    void setUpper(int v){
        WMRange newReference;
        WMRange oldReference;
        do{
            oldReference = rf.get();
            // 检查参数合法性
            if(v < oldReference.lower){
                throw new IllegalArgumentException();
            }
            newReference = new WMRange(v, oldReference.lower);
        }while(!rf.compareAndSet(oldReference, newReference));
    }
}
