package com.learn._01_concurrentFundament;

public class _10_Summary {

    /**
     * 锁，应是私有的、不可变的、不可重用的。
     * 因此 Integer、String 和 Boolean 对象不适合做锁。
     *  - Integer 会缓存 -128～127 这个范围内的数值，每次获取这些数值的对象都是同一个对象。
     *  - String 对象同样会缓存字符串常量到字符串常量池，供重复使用，所以每次获取都是同一个对象。
     */

}
