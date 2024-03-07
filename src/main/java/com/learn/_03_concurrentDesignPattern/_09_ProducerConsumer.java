package com.learn._03_concurrentDesignPattern;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 生产者-消费者设计模式：并发编程领域里，解决分工问题的一种设计模式。
 *  - 实现原理：生产者 - 消费者模式的核心是一个任务队列，
 *    生产者线程生产任务，并将任务添加到任务队列中，而消费者线程从任务队列中获取任务并执行。
 *  - 模式优点：
 *    1）解耦。解耦对于大型系统的设计非常重要，而解耦的一个关键就是组件之间的「依赖关系」和「通信方式」必须受限。
 *       在生产者 - 消费者模式中，生产者和消费者「没有任何依赖关系」，它们彼此之间的通信「只能通过任务队列」，
 *       所以生产者 - 消费者模式是一个不错的解耦方案。
 *    2）支持异步。在生产者 - 消费者模式中，生产者线程只需要将任务添加到任务队列，而无需等待任务被消费者线程执行完，
 *       也就是说任务的生产和消费是异步的，这是与传统的方法之间调用的本质区别，传统的方法之间调用是同步的。
 *    3）能够平衡生产者和消费者的速度差异。假设生产者的速率很慢，而消费者的速率很高，比如是 1:3
 *       使用 3 个生产者线程和 1 个消费者线程即可让生产和消费的速率持平，实现使用适量线程即可达成效果。
 */
public class _09_ProducerConsumer{

}

/**
 * 使用生产者-消费者模式，实现消费者从队列中批量获取任务执行。
 * 场景：批量执行任务，例如要在数据库里 INSERT 1000 条数据。
 *  - 方案对比：
 *   1）第一种方案是用 1000 个线程并发执行，每个线程 INSERT 一条数据；
 *   2）第二种方案是用 1 个线程，执行一个批量的 SQL，一次性把 1000 条数据 INSERT 进去。
 *   显然是第二种方案效率更高。
 *  - 具体场景：
 *    如果每一条回传数据都直接 INSERT 到数据库，那就是上面的第一种方案：每个线程 INSERT 一条数据。
 *    而更好的方案是将每一条回传数据收集起来直到 1000 条数据，再执行一个批量的 SQL 一次性把 1000 条数据 INSERT 进去。
 *  - 方案实现：
 *    将原来直接 INSERT 数据到数据库的线程作为生产者线程，
 *    生产者线程只需将数据添加到任务队列，然后消费者线程负责将任务从任务队列中批量取出并批量执行。
 */
class BatchExecution{
    // 任务队列
    private static BlockingQueue<Runnable> bq =
            new LinkedBlockingQueue<>(2000);

    // 消费者（启动 5 个消费者线程消费）
    public void start(){
        ExecutorService es = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            es.execute(()->{
                while (true){
                    // 获取批量任务
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

    // 从任务队列中获取批量任务
    public List<Runnable> pollTasks() throws InterruptedException {
        LinkedList<Runnable> ts = new LinkedList<>();
        // 阻塞式的从阻塞队列获取一条任务
        Runnable task = bq.take();
        // 首先采用阻塞方式，是因为如果任务队列中没有任务，这样的方式能够避免无谓的循环
        while (task != null){
            ts.add(task);
            // 非阻塞式的从阻塞队列获取一条任务
            task = bq.poll();
        }
        return ts;
    }

    // 执行批量任务
    public void execTasks(List<Runnable> ts){
        // 省略具体实现代码
    }
}


/**
 * 分阶段提交的应用场景
 *  - 写文件如果同步刷盘性能会很慢，所以对于不是很重要的数据，我们往往采用异步刷盘的方式。
 *  - 日志记录的异步刷盘，刷盘时机：
 *   1） ERROR 级别的日志需要立即刷盘；
 *   2） 数据积累到 500 条需要立即刷盘；
 *   3） 存在未刷盘数据，且 5 秒钟内未曾刷盘，需要立即刷盘。
 *  - 实现逻辑：
 *   1）每记录一次日志，都会创建了一个日志任务 LogMsg，并添加到阻塞队列中。
 *   2）消费者线程负责读取阻塞队列中的日志任务，根据刷盘规则将日志记录写入日志文件中。
 */
// 日志级别
enum LEVEL{
    INFO,
    ERROR
}
// 日志记录对象
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

class LoggerTool{
    // 任务队列
    private final BlockingQueue<LogMsg> bq =
            new LinkedBlockingQueue<>(2000);

    // 写日志文件的线程（消费者）
    private final ExecutorService es =
            Executors.newFixedThreadPool(1);

    // 批量写入日志记录的条数
    private static final int BATCH_SIZE = 500;

    // 启动写日志线程（启动消费者线程）
    public void start() throws IOException {
        // 创建日志文件
        File logFile = File.createTempFile("foo", ".log");
        // 开启文件流
        FileWriter logFileWriter = new FileWriter(logFile);
        es.execute(()->{
            try {
                int curIdx = 0;
                long preFT = System.currentTimeMillis();
                while (true) {
                    // 从阻塞队列取一条日志
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
                    // 根据规则判断是否刷盘
                    if (msg != null && msg.level == LEVEL.ERROR ||
                            curIdx == BATCH_SIZE ||
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
                    // 线程被终止时，将所有已写入但是未刷盘的日志都落盘掉
                    logFileWriter.flush();
                    // 关闭文件流
                    logFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 记录 info 级别日志
    public void info(String msg) throws InterruptedException {
        bq.put(new LogMsg(LEVEL.INFO, msg));
    }

    // 记录 error 级别日志
    public void error(String msg) throws InterruptedException {
        bq.put(new LogMsg(LEVEL.ERROR, msg));
    }

    // 毒丸
    private final LogMsg poisonPill = new LogMsg(LEVEL.ERROR, "");
    // 终止日志刷盘的消费者线程
    public void stop() throws InterruptedException {
        // 放入毒丸
        bq.put(poisonPill);
        // 毒丸的好处是能够保证已经进入阻塞队列的日志记录，都被消费完不会丢失掉。
        // 停止消费者线程
        es.shutdown();
    }

}