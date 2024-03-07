package com.learn._03_designPattern;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Copy And Write 写时复制机制
 *
 * CopyOnWriteArrayList 和 CopyOnWriteArraySet
 * 这两个 Copy-on-Write 容器，它们背后的设计思想就是 Copy-on-Write；
 * 通过 Copy-on-Write 这两个容器实现的读操作是无锁的，
 * 由于无锁，所以将读操作的性能发挥到了极致。
 * Copy-on-Write 容器在修改的同时会复制整个容器，所以在提升读操作性能的同时，是以内存复制为代价的。
 *
 * CopyOnWriteArrayList 和 CopyOnWriteArraySet
 * 这两个 Copy-on-Write 容器在修改的时候会复制整个数组，
 * 所以如果容器经常被修改或者这个数组本身就非常大的时候，是不建议使用的。
 * 反之，如果是修改非常少、数组数量也不大，并且对读性能要求苛刻的场景，
 * 使用 Copy-on-Write 容器效果就非常好了。
 *
 **/
public class _02_CopyAndWrite {
}

/**
 * 需求场景：
 *  1. 服务提供方是多实例分布式部署的，客户端在调用某个服务接口时，会选定其中一个服务实例来调用。
 *     所以客户端就需要保存期望调用接口的全部路由信息。
 *  2. 当服务提供方上线或者下线的时候，就需要更新客户端的这张路由信息表。
 *  3. 每次调用都需要访问路由表，所以访问路由表这个操作的性能要求是很高的。
 *     不过路由表对数据的一致性要求并不高，一个服务提供方从上线到反馈到客户端的路由表里，
 *     即便有 5 秒钟，很多时候也都是能接受的。
 *  4. 路由表是典型的读多写少类问题，写操作的量相比于读操作，可谓是沧海一粟，少得可怜。
 *     所以场景的特点是对读的性能要求很高，读多写少，弱一致性，
 *     所以 CopyOnWriteArrayList 和 CopyOnWriteArraySet 很适合这种场景。
 *  5. 服务提供方的每一次上线、下线都会更新路由信息，这时候你有两种选择。
 *     一种是通过更新 Router 的一个状态位来标识，如果这样做，那么所有访问该状态位的地方都需要同步访问，这样很影响性能。
 *     另外一种就是采用 Immutability 模式，每次上线、下线都创建新的 Router 对象或者删除对应的 Router 对象。
 *     由于上线、下线的频率很低，所以后者是最好的选择。
 *
 *  设计：
 *    Router 表示路由信息。
 *    CopyOnWriteArraySet<Router> 表示路由信息表。
 *    ConcurrentHashMap<String, CopyOnWriteArraySet<Router>> 表示接口-路由信息表。
 */
final class Router{
    private final String ip;    // 路由地址
    private final String port;  // 路由端口
    private final String iFace; // 接口名

    public Router(String ip, String port, String iFace) {
        this.ip = ip;
        this.port = port;
        this.iFace = iFace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Router router = (Router) o;
        return Objects.equals(ip, router.ip) &&
                Objects.equals(port, router.port) &&
                Objects.equals(iFace, router.iFace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, iFace);
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getiFace() {
        return iFace;
    }
}

class RouterTable{
    private Map<String, CopyOnWriteArraySet<Router>> table = new ConcurrentHashMap<>();

    // 根据接口名获取路由信息表
    public Set<Router> get(String iFace){
        return table.get(iFace);
    }

    // 服务下线——从路由信息表中删除路由信息
    public void remove(Router router){
        // 获取接口对应的路由信息表
        Set<Router> routers = table.get(router.getiFace());
        // 从路由信息表中删除路由信息
        if (routers != null){
            routers.remove(router);
        }
    }

    // 服务上线——向路由信息表中增加路由信息
    public void add(Router router){
        // 获取/创建接口对应的路由信息表
        Set<Router> routers =
                table.computeIfAbsent(router.getiFace(), k -> new CopyOnWriteArraySet<>());
        // 向路由信息表中增加路由信息
        routers.add(router);
    }
}
