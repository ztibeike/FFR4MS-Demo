package com.zt.filter;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.zuul.context.RequestContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Description 自定义 ribbon 重试路由
 */
@Component
public class RetryRibbonRoutingFilter extends RibbonRoutingFilter {

    @Autowired
    @Qualifier("routingTaskExecutor")
    private ExecutorService taskExecutor;

    // key: service name, value: executing future list
    private Map<String, List<RouteExecuteInfo>> executeFutureMap;

    private static ThreadLocal<RouteExecuteInfo> ROUTE_EXECUTE_THREAD_LOCAL = new TransmittableThreadLocal<>();

    public RetryRibbonRoutingFilter(ProxyRequestHelper helper, RibbonCommandFactory<?> ribbonCommandFactory, List<RibbonRequestCustomizer> requestCustomizers) {
        super(helper, ribbonCommandFactory, requestCustomizers);
        this.executeFutureMap = new ConcurrentHashMap<>();
    }

    @Override
    protected ClientHttpResponse forward(RibbonCommandContext context) throws Exception {
        Map<String, Object> info = this.helper.debug(context.getMethod(), context.getUri(), context.getHeaders(), context.getParams(), context.getRequestEntity());

        try {
            final RouteExecuteInfo executeInfo = new RouteExecuteInfo(null, null, null, context.getServiceId());
            synchronized (this.executeFutureMap) {
                List<RouteExecuteInfo> list = this.executeFutureMap.get(context.getServiceId());
                if (list == null) {
                    list = Collections.synchronizedList(new LinkedList<>());
                    list.add(executeInfo);
                    this.executeFutureMap.put(context.getServiceId(), list);
                } else {
                    list.add(executeInfo);
                }
            }
            ROUTE_EXECUTE_THREAD_LOCAL.set(executeInfo);
            Future<ClientHttpResponse> future = taskExecutor.submit(() -> this.ribbonCommandFactory.create(context).execute());
            executeInfo.future = future;

            ClientHttpResponse response;
            try {
                response = future.get();
            } catch (InterruptedException | CancellationException | ExecutionException e) {
                // 重新请求
                RibbonCommandContext retryContext = this.buildCommandContext(RequestContext.getCurrentContext());
                response = this.ribbonCommandFactory.create(retryContext).execute();
            }
            this.helper.appendDebug(info, response.getRawStatusCode(), response.getHeaders());
            return response;
        } catch (HystrixRuntimeException var5) {
            return this.handleException(info, var5);
        } finally {
            final RouteExecuteInfo executeInfo = ROUTE_EXECUTE_THREAD_LOCAL.get();
            if (executeInfo != null && StringUtils.hasText(executeInfo.host)) {
                synchronized (this.executeFutureMap.get(context.getServiceId())) {
                    this.executeFutureMap.get(context.getServiceId())
                            .remove(new RouteExecuteInfo(null, executeInfo.host, executeInfo.port, executeInfo.serviceName));
                }
            }
        }
    }

    /**
     * 关闭指定的服务请求
     *
     * @param serviceName
     */
    public void downServiceFuture(String serviceName, String host, Integer port) {
        final List<RouteExecuteInfo> executeInfoList = executeFutureMap.get(serviceName);
        if (!CollectionUtils.isEmpty(executeInfoList)) {
            synchronized (executeInfoList) {
                RouteExecuteInfo info = new RouteExecuteInfo(null, host, port, serviceName);
                final List<RouteExecuteInfo> collect = executeInfoList.stream()
                        .filter(routeExecuteInfo -> routeExecuteInfo.equals(info))
                        .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(collect)) {
                    collect.forEach(routeExecuteInfo -> {
                        try {
                            routeExecuteInfo.future.cancel(true);
                        } catch (Exception e) {}
                    });
                }
            }
        }
    }

    public static RouteExecuteInfo getContextExecuteInfo() {
        return RetryRibbonRoutingFilter.ROUTE_EXECUTE_THREAD_LOCAL.get();
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode(exclude = "future")
    public static class RouteExecuteInfo {

        private Future future;

        @Setter
        private String host;

        @Setter
        private Integer port;

        @Setter
        private String serviceName;

    }

}
