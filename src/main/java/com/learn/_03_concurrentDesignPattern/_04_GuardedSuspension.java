package com.learn._03_concurrentDesignPattern;


import com.learn.common.CommTools;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/**
 * Guarded Suspension（保护性地暂停）模式：
 *  - 等待唤醒机制的规范实现模式
 *
 * 需求场景：
 *  - 用户通过浏览器发过来一个请求，会被转换成一个异步消息发送给 MQ，等 MQ 返回结果后，再将这个结果返回至浏览器。
 *  - 发送异步消息的线程 T1 如何异步转同步的等待 MQ 消费完消息，然后获取响应信息结果返回。
 *
 *  Guarded Suspension 设计模式：
 *   1）组成部分：
 *     - 受保护对象
 *     - get(Predicate<T> p) 线程阻塞等待条件满足
 *     - onChange(T t)       改变目标对象的状态
 *   2）实现原理：
 *     - get() 方法通过条件变量的 await() 方法实现等待
 *     - onChanged() 方法通过条件变量的 signalAll() 方法实现唤醒功能
 *
 *  拓展问题：
 *    - handleWebReq() 里面创建了 GuardedObject 对象的实例 go，并调用其 get() 方等待结果，
 *      那在 onMessage() 方法中，如何才能够找到匹配的 GuardedObject 对象，调用 onChange() 方法执行事件通知呢？
 *    - 通过消息的唯一 id 进行标记具体的 GuardedObject, 提供可以通过消息唯一 id 找到具体 GuardedObject 对象，
 *      进而调用 onChange() 事件通知的方法。
 *  拓展理解：
 *    - 属于多线程版本的 if。
 *      单线程场景中，if 语句是不需要等待的，因为在只有一个线程的条件下，如果这个线程被阻塞，
 *      那就没有其他活动线程了，这意味着 if 判断条件的结果也不会发生变化了。
 *      但是多线程场景中，等待就变得有意义了，这种场景下，if 判断条件的结果是可能发生变化的。
 */
public class _04_GuardedSuspension {
    /* 客户端 */
    public static Message handleWebRequest(Message message){
        // 1. 发送消息给 MQ
        send(message);
        // 2. 等待 MQ 消费完成返回消息
        GuardedObject<Message> go = GuardedObject.create(message.getId());
        Message result = go.get(t -> t != null);
        // 3. 返回响应消息结果
        return result;
    }
    public static void send(Message message){
        // 客户端完成发送
        // 启动 MQ 异步的消费消息
        MQConsumerMessage(message);
    }

    /* 服务端 */
    private static ExecutorService executor = Executors.newFixedThreadPool(3);
    private static Random random = new Random();
    public static void MQConsumerMessage(Message message){
        executor.execute(()->{
            int timeout = random.nextInt(6);
            // 模拟消费耗时
            CommTools.sleep(timeout, TimeUnit.SECONDS);
            // 生成服务端的响应消息
            Message result = new Message(message);
            result.setStatus("YES");
            result.setProcessTimeout(timeout);
            onMessage(result);
        });
    }
    // MQ 消费完成后调用的方法
    public static void onMessage(Message message){
        GuardedObject.fireEvent(message.getId(), message);
    }

    public static void main(String[] args) throws InterruptedException {
        AtomicLong atomicLong = new AtomicLong(100);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            pool.execute(()->{
                long id = atomicLong.getAndIncrement();
                Message req = new Message(String.valueOf(id), "{...}");
                Message resp = handleWebRequest(req);
                System.out.println(req + "  :  " + resp);
                latch.countDown();
            });
        }
        latch.await();
        pool.shutdown();
        executor.shutdown();
    }

}

class GuardedObject<T>{
    // 受保护的对象
    private T obj;
    private final Lock lock = new ReentrantLock();
    private final Condition done = lock.newCondition();
    // MQ 消息 id 和 GuardedObject 对象实例的关系：<Key: MQ 消息 id, Value: GuardedObject 对象实例>
    private static final Map<Object, GuardedObject> gos = new ConcurrentHashMap<>();

    // 创建一个 GuardedObject 对象，并使用消息的唯一 id 记录
    public static <K> GuardedObject create(K key){
        GuardedObject go = new GuardedObject();
        gos.put(key, go);
        return go;
    }

    // 通过消息的唯一 id 找到具体等待的 GuardedObject 对象，执行事件通知操作
    public static <K, T> void fireEvent(K key, T obj){
        GuardedObject go = gos.get(key);
        if (go != null){
            go.onChange(obj);
        }
    }

    // 异步转同步的等待方法
    // 线程阻塞直到条件满足，通过条件变量的 await() 方法实现等待
    public T get(Predicate<T> p){
        lock.lock();
        try {
            while (!p.test(obj)){
                done.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            lock.unlock();
        }
        // 返回非空的受保护对象
        return obj;
    }

    // 事件通知方法
    // 改变受保护对象的状态，通过条件变量的 signalAll() 方法实现唤醒功能
    public void onChange(T obj){
        lock.lock();
        try {
            this.obj = obj;
            done.signalAll();
        }finally {
            lock.unlock();
        }
    }
}

// 消息实体
class Message{
    private String id; // 每个消息的唯一 id
    private String message;
    private String status;
    private int processTimeout;

    public Message(String id, String message) {
        this.id = id;
        this.message = message;
        this.status = "NO";
    }

    public Message(Message message) {
        this.id = message.id;
        this.message = message.message;
        this.status = message.status;
    }

    public String getId() {
        return id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setProcessTimeout(int processTimeout) {
        this.processTimeout = processTimeout;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", status='" + status + '\'' +
                ", processTimeout=" + processTimeout +
                '}';
    }
}


