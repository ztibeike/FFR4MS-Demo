package com.zt.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.zt.constant.ZuulLinkTraceConstant;
import com.zt.utils.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description 微服务链路追踪信息过滤器
 */
@Slf4j
@Component
public class MicroServiceLinkTraceInfoFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
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
        RequestContext context = RequestContext.getCurrentContext();

        HttpServletRequest request = context.getRequest();

        String tId = request.getHeader(ZuulLinkTraceConstant.TRACE_ID_HEADER);
        if (StringUtils.isEmpty(tId)) {
            tId = IdUtils.getUUId();

            if (log.isDebugEnabled()) {
                log.debug("Generated trace id: {}", tId);
            }
        }

        context.addZuulRequestHeader(ZuulLinkTraceConstant.TRACE_ID_HEADER, tId);

        return null;
    }
}
