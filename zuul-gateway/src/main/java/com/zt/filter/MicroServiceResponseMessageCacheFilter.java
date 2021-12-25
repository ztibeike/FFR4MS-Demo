package com.zt.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.exception.ZuulException;
import com.zt.context.ResponseInfoCacheContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

/**
 * @Description 微服务响应报文缓存过滤器
 */
@Slf4j
@Component
public class MicroServiceResponseMessageCacheFilter extends ZuulFilter {

    @Autowired
    private ResponseInfoCacheContext cacheContext;

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        try {
            cacheContext.set();
        } catch (Exception e) {
            log.error("Response message cache error", e);
        }
        return null;
    }

}
