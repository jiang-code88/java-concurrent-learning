package com.learn._03_designPattern;

import com.sun.org.apache.xpath.internal.operations.String;

/**
 * 不变性（Immutability）模式
 *
 * 如果共享变量只有读操作，没有写操作，那么就不存在并发问题。
 * 这上升到一种解决并发问题的设计模式：不变性（Immutability）模式
 *  - 对象一旦被创建之后，状态就不再发生变化。
 *    换句话说，就是变量一旦被赋值，就不允许修改了（没有写操作）；没有修改操作，也就是保持了不变性。
 *
 * 如何实现「不可变性类」
 * 将一个类所有的属性都设置成 final 的，并且只允许存在只读方法，那么这个类基本上就具备不可变性了。
 * 更严格的做法是这个类本身也是 final 的，也就是不允许继承。
 *  - JDK 中 String 和 Long、Integer、Double 等基础类型的包装类都具备不可变性，是线程安全的。
 *    类和属性都是 final 的，所有方法均是只读的。
 *
 */
public class _01_Immutability {
    public static void main(String[] args) {

    }
}
