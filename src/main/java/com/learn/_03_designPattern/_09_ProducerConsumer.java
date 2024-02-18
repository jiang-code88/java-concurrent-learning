package com.learn._03_designPattern;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 生产者-消费者模式
 */
public class _09_ProducerConsumer{

}

/**
 * 分阶段提交任务，使用消费者消费异步的消费任务
 */
// 日志级别
enum LEVEL{
    INFO,
    ERROR
}
// 日志对象
class LogMsg{
    LEVEL level;
    String msg;
    public LogMsg(LEVEL level, String msg) {
        this.level = level;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "LogMsg{" +
                "level=" + level +
                ", msg='" + msg + '\'' +
                '}';
    }
}

/**
 * 1. ERROR 级别的日志需要立即刷盘；
 * 2. 数据积累到 500 条需要立即刷盘；
 * 3. 存在未刷盘数据，且 5 秒钟内未曾刷盘，需要立即刷盘。
 */
class LoggerTool{

    // 任务队列
    private final BlockingQueue<LogMsg> bq =
            new LinkedBlockingQueue<>(2000);

    // 写日志的线程
    private final ExecutorService es =
            Executors.newFixedThreadPool(1);

    // 批量写入的条数
    private static final int batchSize = 500;

    // 启动写日志
    public void start() throws IOException {
        File logFile = File.createTempFile("foo", ".log");

        FileWriter logFileWriter = new FileWriter(logFile);

        es.execute(()->{
            try {
                int curIdx = 0;
                long preFT = System.currentTimeMillis();
                while (true) {
                    LogMsg msg = bq.poll(5, TimeUnit.SECONDS);

                    // 如果是毒丸，终止线程的执行
                    if (poisonPill.equals(msg)){
                        break;
                    }
                    // 写日志
                    if (msg != null) {
                        logFileWriter.write(msg.toString());
                        curIdx++;
                    }

                    // 存在未刷盘的数据才刷盘
                    if (curIdx <= 0) {
                        continue;
                    }
                    // 按照规则刷盘
                    if (msg != null && msg.level == LEVEL.ERROR ||
                            curIdx == batchSize ||
                            System.currentTimeMillis() - preFT > 5000) {
                        logFileWriter.flush();
                        curIdx = 0;
                        preFT = System.currentTimeMillis();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    logFileWriter.flush();
                    logFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void info(String msg) throws InterruptedException {
        bq.put(new LogMsg(LEVEL.INFO, msg));
    }

    public void error(String msg) throws InterruptedException {
        bq.put(new LogMsg(LEVEL.ERROR, msg));
    }

    // 毒丸
    private final LogMsg poisonPill = new LogMsg(LEVEL.ERROR, "");

    // 终止日志刷盘的消费者线程
    public void stop() throws InterruptedException {
        // 放入毒丸
        bq.put(poisonPill);
        // 停止消费者线程
        es.shutdown();
    }

}

/**
 * 使用生产者-消费者模式，实现消费者从队列中批量获取任务执行。
 */
class BatchExecution{
    // 任务队列
    private static BlockingQueue<Runnable> bq =
            new LinkedBlockingQueue<>(2000);

    // 从任务队列中批量取出任务
    public void start(){
        // 生产者
        ExecutorService es = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            es.execute(()->{
                while (true){
                    // 批量的获取任务
                    List<Runnable> ts = null;
                    try {
                        ts = pollTasks();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // 执行批量任务
                    execTasks(ts);
                }
            });
        }
    }

    // 执行批量任务
    public List<Runnable> pollTasks() throws InterruptedException {
        LinkedList<Runnable> ts = new LinkedList<>();
        Runnable task = bq.take();
        // 阻塞式的获取一条任务
        while (task != null){
            ts.add(task);
            // 非阻塞式的获取一条任务
            task = bq.poll();
        }
        return ts;
    }

    // 批量执行任务
    public void execTasks(List<Runnable> ts){
        // 省略具体实现代码
    }
}
