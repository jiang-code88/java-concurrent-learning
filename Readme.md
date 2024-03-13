# Java 并发编程学习
基于的课程：
- 极客时间 《Java 并发编程实战》王宝令

# 项目代码目录
```
.
├── Readme.md
├── pom.xml
└── src
    └── main
        └── java
            └── com
                └── learn
                    ├── _01_concurrentFundament
                    │   ├── _01_ConcurrentProblems.java  # 重现可见性和原子性问题（多线程累加整数）
                    │   ├── _02_SynchronizedGuide.java   # Synchronized 锁的使用示例
                    │   ├── _03_MultiLock.java           # 细粒度锁的使用示例
                    │   ├── _04_ClassLock.java           # 类对象锁的使用示例
                    │   ├── _05_deadLock.java            # 重现和解决死锁问题
                    │   ├── _06_waitNotify.java          # synchronized 锁的等待-通知机制
                    │   ├── _07_Monitor.java             # 「锁+条件变量」实现等待-通知机制
                    │   ├── _08_ThreadStatus.java        # Java 线程状态转换讲解
                    │   ├── _09_Counter.java             # 如何使用面向对象的思想写并发程序
                    │   └── _10_Summary.java             # 锁是私有的、不可变的、不可重用的
                    ├── _02_concurrentUtils
                    │   ├── _01_LockCondition_1.java     # ReentryLock 的使用指南
                    │   ├── _02_LockCondition_2.java     # ReentryLock 实现异步转同步
                    │   ├── _03_Semaphore.java           # 信号量
                    │   ├── _04_ReadWriteLock.java       # 读写锁
                    │   ├── _05_StampedLock.java         # ReadWriteLock 的性能升级版
                    │   ├── _06_CountDownLatchAndCyclicBarrier.java # 线程同步工具 CountDownLatch 和其升级版 CyclicBarrier
                    │   ├── _07_Container.java           # 同步容器和其升级版并发容器
                    │   ├── _08_Atomic.java              # 原子类工具和 CAS 操作
                    │   ├── _09_Executor.java            # 线程池的使用指南和工作原理
                    │   ├── _10_Future.java              # Future 获取线程的执行结果 
                    │   ├── _11_CompletableFuture.java   # 高级复杂的异步执行工具
                    │   ├── _12_CompletionService.java   # 获取批量异步任务的返回结果
                    │   └── _13_ForkJoinMapReduce.java   # 分治并发任务的计算框架 ForkJoinPool
                    ├── _03_concurrentDesignPattern
                    │   ├── _01_Immutability.java        # 不可变性类解决并发问题
                    │   ├── _02_CopyAndWrite.java        # 写时复制解决并发问题
                    │   ├── _03_ThreadLocal.java         # ThreadLocal 的使用指南
                    │   ├── _04_GuardedSuspension.java   # 多线程版本的 if（等待唤醒机制实现）
                    │   ├── _05_Balking.java             # 多线程版本的 if（互斥锁实现）
                    │   ├── _06_ThreadPerMessage.java    # 分工问题设计模式（网络服务器常用实现方式）
                    │   ├── _07_WorkerThread.java        # 分工问题设计模式（线程池常用实现方式）
                    │   ├── _08_TwoStageTermination.java # 两阶段优雅停止线程模式
                    │   └── _09_ProducerConsumer.java    # 分工问题设计模式（生产者-消费者模式）
                    ├── _04_concurrentCaseAnalysis
                    │   └── _01_RateLimiter.java        # 高性能限流器工作机制（令牌桶算法）
                    └── common
                        ├── Account.java                # 抽象工具类
                        └── CommTools.java              # 工具方法类
```