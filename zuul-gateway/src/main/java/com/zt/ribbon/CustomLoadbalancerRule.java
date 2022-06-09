package com.zt.ribbon;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.Server;
import com.zt.filter.RetryRibbonRoutingFilter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description 标记负载均衡算法
 */
@Slf4j
@Component
public class CustomLoadbalancerRule extends RandomRule {

    // key: serviceId value: micro server list information
    private Map<String, MicroServiceList> serverListMap = new HashMap<>();

    /**
     * 选择服务实例
     *
     * @param lb
     * @param key
     * @return
     */
    @Override
    public Server choose(ILoadBalancer lb, Object key) {
        Server server = null;
        if (lb instanceof BaseLoadBalancer) {
            BaseLoadBalancer loadBalancer = (BaseLoadBalancer) lb;

            String serviceName = loadBalancer.getName();

            MicroServiceList serviceList = this.serverListMap.get(serviceName);
            if (serviceList != null) {
                // 获取优先服务实例列表
                List<MarkServer> priorityServerList = serviceList.markPriorityServerList;
                if (!CollectionUtils.isEmpty(priorityServerList)) {
                    // 优先选择服务
                    server = priorityChoose(serviceName, lb);
                }
            }
            if (server == null) {
                server = randomServer(lb.getReachableServers(), serviceName);
            }
        }
        /*
        if (server == null) {
            server = super.choose(lb, key);
        }*/
        if (server != null) {
            // 注入上下文
            final RetryRibbonRoutingFilter.RouteExecuteInfo executeInfo = RetryRibbonRoutingFilter.getContextExecuteInfo();
            if (executeInfo != null) {
                executeInfo.setHost(server.getHost());
                executeInfo.setPort(server.getPort());
            }
        }
        return server;
    }

    /**
     * 随机选择服务
     *
     * @param serverList
     * @return
     */
    private Server randomServer(List<Server> serverList, String serviceName) {
        serverList = serverList.stream().filter(server -> {
            if (this.serverListMap.get(serviceName) != null) {
                synchronized (this.serverListMap.get(serviceName).faultServerList) {
                    List<MarkServer> faultServerList = this.serverListMap.get(serviceName).faultServerList;
                    for (MarkServer markServer : faultServerList) {
                        if (markServer.getHost().equals(server.getHost()) && markServer.getPort().equals(server.getPort())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(serverList)) {
            return null;
        }
        Server server = serverList.get(new Random().nextInt(serverList.size()));
        return server.isAlive() ? server : null;
    }

    /**
     * 优先选择服务实例
     *
     * @param serviceName
     * @param lb
     * @return
     */
    private Server priorityChoose(String serviceName, ILoadBalancer lb) {
        List<MarkServer> list = new ArrayList<>(this.serverListMap.get(serviceName).markPriorityServerList.size());
        synchronized (this.serverListMap.get(serviceName).markPriorityServerList) {
            list.addAll(this.serverListMap.get(serviceName).markPriorityServerList);
        }

        if (list.size() > 0) {
            // 取可用的服务
            List<Server> servers = lb.getReachableServers();
            list = list.stream().filter(markServer -> {
                for (Server server : servers) {
                    if (markServer.getHost().equals(server.getHost()) && markServer.getPort().equals(server.getPort()) && server.isAlive()) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());

            if (list.size() > 0) {
                // 取随机服务
                List<MarkServer> finalList = list;
                List<Server> collect = servers.stream().filter(server -> {
                    for (MarkServer markServer : finalList) {
                        if (markServer.getHost().equals(server.getHost()) && markServer.getPort().equals(server.getPort())) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
                return randomServer(collect, serviceName);
            }
        }
        return null;
    }

    /**
     * 标记服务
     *
     * @param server
     * @param type
     */
    public void markServer(MarkServer server, MarkServerType type) {
        Assert.notNull(server, "Mark server not be null");
        Assert.notNull(type, "Mark type not be null");

        // 初始化标记服务列表
        initServerList(server.getServiceName());

        if (MarkServerType.SERVER_PRIORITY == type) {
            // 标记服务优先
            synchronized (this.serverListMap.get(server.getServiceName()).markPriorityServerList) {
                List<MarkServer> priorityServerList = this.serverListMap.get(server.getServiceName()).markPriorityServerList;
                if (!priorityServerList.contains(server)) {
                    priorityServerList.add(server);
                }
            }
            // 删除下线服务
            synchronized (this.serverListMap.get(server.getServiceName()).faultServerList) {
                this.serverListMap.get(server.getServiceName()).faultServerList.remove(server);
            }
        } else if (MarkServerType.SERVER_DOWN == type) {
            // 标记服务下线
            synchronized (this.serverListMap.get(server.getServiceName()).faultServerList) {
                List<MarkServer> faultServerList = this.serverListMap.get(server.getServiceName()).faultServerList;
                if (!faultServerList.contains(server)) {
                    faultServerList.add(server);
                }
            }
            // 移除优先列表
            synchronized (this.serverListMap.get(server.getServiceName()).markPriorityServerList) {
                List<MarkServer> priorityServerList = this.serverListMap.get(server.getServiceName()).markPriorityServerList;
                priorityServerList.remove(server);
            }
        } else if (MarkServerType.SERVER_UP == type) {
            // 标记服务上线, 从错误列表中移除服务
            synchronized (this.serverListMap.get(server.getServiceName()).faultServerList) {
                List<MarkServer> faultServerList = this.serverListMap.get(server.getServiceName()).faultServerList;
                faultServerList.remove(server);
            }
        }
    }

    /**
     * 初始化服务名称的服务列表
     *
     * @param serviceName
     */
    private synchronized void initServerList(String serviceName) {
        if (this.serverListMap.get(serviceName) == null) {
            synchronized (this.serverListMap) {
                if (this.serverListMap.get(serviceName) == null) {
                    this.serverListMap.put(serviceName, new MicroServiceList(serviceName, new ArrayList<>(4), new ArrayList<>(4), new ArrayList<>(4)));
                }
            }
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @Getter
    public static class MicroServiceList {

        private String serviceId;

        private List<MarkServer> markPriorityServerList;

        private List<MarkServer> faultServerList;

        private List<MarkServer> allServerList;

    }

    @AllArgsConstructor
    @Getter(AccessLevel.PACKAGE)
    @EqualsAndHashCode
    public static class MarkServer {

        private String host;

        private Integer port;

        private String serviceName;

    }

    public enum MarkServerType {

        /**
         * 服务优先
         */
        SERVER_PRIORITY,
        /**
         * 服务下线
         */
        SERVER_DOWN,
        /**
         * 服务上线
         */
        SERVER_UP
    }

}

