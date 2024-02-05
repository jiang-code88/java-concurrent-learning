package com.learn._03_designPattern;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Copy And Write 写时复制机制
 *
 * // TODO：重写类的 equals 和 hashcode 的原理，我已经全忘记了。
 */
public class _02_CopyAndWrite {
}

// 路由信息
final class Router{
    private final String ip;
    private final String port;
    private final String iFace; // 接口

    public Router(String ip, String port, String iFace) {
        this.ip = ip;
        this.port = port;
        this.iFace = iFace;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Router)) return false;
        Router router = (Router) o;
        return Objects.equals(ip, router.ip)
                && Objects.equals(port, router.port)
                && Objects.equals(iFace, router.iFace);
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

    // 根据接口获取路由表
    public Set<Router> get(String iFace){
        return table.get(iFace);
    }

    // 删除路由
    public void remove(Router router){
        Set<Router> routers = table.get(router.getiFace());
        if (routers != null){
            routers.remove(router);
        }
    }

    // 增加路由
    public void add(Router router){
        Set<Router> routers =
                table.computeIfAbsent(router.getiFace(), k -> new CopyOnWriteArraySet<>());
        routers.add(router);
    }
}
