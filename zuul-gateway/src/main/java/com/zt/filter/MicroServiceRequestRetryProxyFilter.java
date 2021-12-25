package com.zt.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.zt.constant.ZuulLinkTraceConstant;
import com.zt.context.ResponseInfoCacheContext;
import com.zt.context.ZuulRequestContext;
import com.zt.model.MicroServiceResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description 微服务请求重试代理过滤器
 */
@Slf4j
@Component
public class MicroServiceRequestRetryProxyFilter extends ZuulFilter {

    private static final Map<String, String> IGNORE_SET_HEADER;

    static {
        IGNORE_SET_HEADER = new HashMap<>(2);
        IGNORE_SET_HEADER.put("connection", "keep-alive");
        IGNORE_SET_HEADER.put("transfer-encoding", "chunked");
    }

    @Autowired
    private ResponseInfoCacheContext cacheContext;

    @Override
    public String filterType() {
        return FilterConstants.ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return -1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        if (StringUtils.isEmpty(ZuulRequestContext.getRequestHeader(ZuulLinkTraceConstant.SERVICE_NAME_HEADER))) {
            // 无服务名称，跳过
            return null;
        }
        MicroServiceResponseModel model = cacheContext.get();

        if (model != null) {
            if (log.isDebugEnabled()) {
                log.debug("Hit response cache, return directly");
            }
            RequestContext context = ZuulRequestContext.getContext();
            // 网关不进行转发
            context.setSendZuulResponse(false);
            // 设置响应码
            context.setResponseStatusCode(model.getResponseCode());
            // 设置响应体
            context.setResponseBody(model.getBody());
            // 设置响应头
            if (!CollectionUtils.isEmpty(model.getHeaders())) {
                model.getHeaders().entrySet().forEach(entry -> {
                    if (!CollectionUtils.isEmpty(entry.getValue())) {
                        entry.getValue().forEach(value -> {
                            if (StringUtils.isEmpty(value)) {
                                return;
                            }
                            // 忽略自带返回的响应头
                            String ignoreValue = IGNORE_SET_HEADER.get(entry.getKey().toLowerCase());
                            if (ignoreValue == null || !ignoreValue.equals(value.toLowerCase())) {
                                context.addZuulResponseHeader(entry.getKey(), value);
                            }
                        });
                    }
                });
            }

            context.addZuulResponseHeader(ZuulLinkTraceConstant.ZUUL_RESPONSE_CACHE_BACK, Boolean.TRUE.toString());
        }

        return null;
    }
}
