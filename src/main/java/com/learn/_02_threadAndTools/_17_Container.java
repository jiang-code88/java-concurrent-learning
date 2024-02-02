package com.learn._02_threadAndTools;

import java.util.*;

/**
 * Java 并发容器（可以保障线程安全的容器类）
 *   - Java 中的容器主要可以分为四个大类 List、Map、Set 和 Queue
 * 同步容器：（Java 1.5 之前提供的线程安全的容器，但是性能很差，因为是基于 synchronized 同步关键字实现的）
 *   - 如何将非线程安全的容器变成线程安全容器：
 *     将非线程安全容器（基于 synchronized 关键字）封装在对象内部，然后控制好访问路径。
 *   - JDK 提供按照以上原则，在 Collections 类中提供了一套完备的包装类，
 *     基于 synchronized 包装后提供线程安全的「同步容器」。
 *   - Java 提供的同步容器还有 Vector、Stack 和 Hashtable，
 *     这三个容器不是基于包装类实现的，但同样是基于 synchronized 实现的
 * 并发容器：（Java 在 1.5 及之后版本提供了性能更高的容器，避免所有方法都用 synchronized 来保证互斥，串行度太高）
 *   - List：
 *     1）CopyOnWriteArrayList：写时会将共享变量新复制一份出来，好处是读操作完全无锁。
 *        仅适用于写操作非常少的场景，而且能够容忍读写的短暂不一致，遍历的迭代器是只读的，不支持增删改。
 *   - Map：
 *     1）ConcurrentHashMap：key 无序
 *     2）ConcurrentSkipListMap：key 有序
 *     两个均 key 和 value 不允许为 null，在并发程度非常高的情况下，
 *     如果对 ConcurrentHashMap 的性能还不满意，可以尝试一下 ConcurrentSkipListMap。
 *   - Set：
 *     1）CopyOnWriteArraySet
 *     2）ConcurrentSkipListSe
 *
 *   - Queue：单端队列，只能队尾入队，队首出队
 *   - Deque：双端队列，队尾和队首皆可入队，出队
 *     （Blocking 阻塞：当队列已满时，入队操作阻塞；当队列已空时，出队操作阻塞。阻塞队列用 Blocking 关键字标识）
 *
 *     单端阻塞
 *     1）ArrayBlockingQueue 基于数组实现
 *     2）LinkedBlockingQueue 基于链表实现
 *     3）PriorityBlockingQueue 支持优先级出队
 *     4）SynchronousQueue 不持有队列
 *     5）LinkedTransferQueue 性能更好
 *     1）DelayQueue 支持延时出队
 *
 *     双端阻塞
 *     1）LinkedBlockingDeque
 *
 *     单端非阻塞
 *     1）ConcurrentLinkedQueue
 *
 *     双端非阻塞
 *     1）ConcurrentLinkedDeque
 *
 *     实际工作中，一般都不建议使用无界的队列，因为数据量大了之后很容易导致 OOM。
 *     只有 ArrayBlockingQueue 和 LinkedBlockingQueue 是支持有界的。
 */
public class _17_Container {
    public static void main(String[] args) {
        // 1 同步容器（Java 1.5 版本前提供的）
        // 分别把 ArrayList、HashSet 和 HashMap 包装成了线程安全的 List、Set 和 Map 同步容器
        List<String> list = Collections.synchronizedList(new ArrayList<>());
        Set<String> Set = Collections.synchronizedSet(new HashSet<>());
        Map<String, String> map = Collections.synchronizedMap(new HashMap<>());

        // Java 直接提供的同步容器（这三个容器不是基于包装类实现的，但同样是基于 synchronized 实现的）
        Vector vector = new Vector();
        Stack stack = new Stack();
        Hashtable hashtable = new Hashtable();

        // 同步容器中容器被忽视的坑是多线程迭代器遍历容器，存在并发问题，组合操作不具备原子性。
        // 反例：（因为是对 list 的操作，如果 list 变化了，会使得迭代器报错，所以先把 list 锁住，
        //        保证迭代器运行期间 list 不会变化。这也是在迭代器里对当前元素删除会报错的原因）
        Iterator<String> i = list.iterator();
        while (i.hasNext()){
            foo(i.next());
        }
        // 正例：(锁 list 对象的原因是，list 同步容器中 get add 的改变操作，
        //       去锁的对象也是容器 list 本身，所以迭代器遍历时锁 list 可以防止 list 不会再迭代期间被改变)
        synchronized (list){
            Iterator<String> iterator = list.iterator();
            while (iterator.hasNext()){
                foo(iterator.next());
            }
        }
    }

    public static void foo(String s){}
}

// 基于 synchronized 关键字，将非线程安全容器 ArrayList 封装在对象内部，并提供互斥的访问路径变成线程安全容器
class SafeArrayList<T>{
    // 分装 ArrayList
    private List<T> list = new ArrayList<>();

    // 控制访问路径

    public synchronized T get(int idx){
        return list.get(idx);
    }

    public synchronized void add(int idx, T t){
        list.add(idx, t);
    }

    public synchronized boolean addIfNotExist(T t){
        if (list.contains(t)) {
            return false;
        }
        list.add(t);
        return true;
    }
}
