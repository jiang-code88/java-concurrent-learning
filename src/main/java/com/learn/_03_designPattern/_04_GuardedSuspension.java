package com.learn._03_designPattern;


import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/**
 * Guarded Suspension 模式：保护性地暂停
 */
public class _04_GuardedSuspension {
    private static ExecutorService executor = Executors.newFixedThreadPool(3);
    private static Random random = new Random();

    // 客户端
    public static Message handleWebRequest(Message message){
        // 1. 发送消息给 MQ
        send(message);
        // 2. 等待 MQ 消费完成返回消息
        GuardedObject<Message> go = GuardedObject.create(message.getId());
        Message result = go.get(t -> t != null);
        return result;
    }
    public static void send(Message message){
        executor.execute(()->{
            int timeout = random.nextInt(6);
            try {
                TimeUnit.SECONDS.sleep(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Message result = new Message(message);
            result.setStatus("YES");
            result.setProcessTimeout(timeout);
            onMessage(result);
        });
    }

    // 服务端
    // MQ 消费完成时调用的方法
    public static void onMessage(Message message){
        GuardedObject.fireEvent(message.getId(), message);
    }

    public static void main(String[] args) {
        AtomicLong atomicLong = new AtomicLong(100);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            pool.execute(()->{
                long id = atomicLong.getAndIncrement();
                Message req = new Message(String.valueOf(id), "{...}");
                Message resp = handleWebRequest(req);
                System.out.println(req + "  :  " + resp);
            });
        }
        // pool.shutdown();
        // executor.shutdown();
    }

}

class GuardedObject<T>{
    private T obj;
    private final Lock lock = new ReentrantLock();
    private final Condition done = lock.newCondition();
    private final long timeout = 1;
    private static final Map<Object, GuardedObject> gos = new ConcurrentHashMap<>();

    public static <K> GuardedObject create(K key){
        GuardedObject go = new GuardedObject();
        gos.put(key, go);
        return go;
    }

    public static <K, T> void fireEvent(K key, T obj){
        GuardedObject go = gos.get(key);
        if (go != null){
            go.onChange(obj);
        }
    }

    // 异步转同步的等待方法
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
        return obj;
    }

    // 事件通知方法
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
    private String id;
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


