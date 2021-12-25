package com.zt.filter;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.zt.context.ZuulRequestContext;
import com.zt.dto.ReplaceServiceInstanceRequestDTO;
import com.zt.ribbon.CustomLoadbalancerRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ZuulApiHandlerFilter extends ZuulFilter {

    @Autowired
    private RetryRibbonRoutingFilter routingFilter;

    @Autowired
    private CustomLoadbalancerRule loadbalancerRule;

    private String replaceUri = "/zuulApi/replaceServiceInstance";

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return -10;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext context = ZuulRequestContext.getContext();
        final String requestURI = context.getRequest().getRequestURI();
        if (!replaceUri.equals(requestURI)) {
            return null;
        }
        // 网关不进行转发
        context.setSendZuulResponse(false);
        // 设置响应码
        context.setResponseStatusCode(HttpStatus.OK.value());

        Map<String, String> result = new HashMap<>();
        try {
            final HttpServletRequest request = ZuulRequestContext.getRequest();
            final ServletInputStream stream = request.getInputStream();
            byte[] buff = new byte[request.getContentLength()];
            IOUtils.read(stream, buff, 0, request.getContentLength());

            // 请求
            final ReplaceServiceInstanceRequestDTO requestDTO = JSONObject.parseObject(new String(buff, StandardCharsets.UTF_8), ReplaceServiceInstanceRequestDTO.class);
            // 先替换实例
            if (StringUtils.hasText(requestDTO.getReplaceInstanceHost()) && requestDTO.getReplaceInstancePort() != null) {
                // 标记优先实例
                final CustomLoadbalancerRule.MarkServer upServer = new CustomLoadbalancerRule.MarkServer(requestDTO.getReplaceInstanceHost(), requestDTO.getReplaceInstancePort(), requestDTO.getServiceName());
                this.loadbalancerRule.markServer(upServer, CustomLoadbalancerRule.MarkServerType.SERVER_PRIORITY);
            }
            // 标记下线实例
            if (StringUtils.hasText(requestDTO.getDownInstanceHost()) && requestDTO.getDownInstancePort() != null) {
                final CustomLoadbalancerRule.MarkServer downServer = new CustomLoadbalancerRule.MarkServer(requestDTO.getDownInstanceHost(), requestDTO.getDownInstancePort(), requestDTO.getServiceName());
                this.loadbalancerRule.markServer(downServer, CustomLoadbalancerRule.MarkServerType.SERVER_DOWN);
                // 再通知正在请求的路由信息
                this.routingFilter.downServiceFuture(requestDTO.getServiceName(), requestDTO.getDownInstanceHost(), requestDTO.getDownInstancePort());
            }

            result.put("code", "000000");
            result.put("msg", "操作成功");
        } catch (Exception e) {
            log.error("切换实例错误", e);
            result.put("code", "000001");
            result.put("msg", e.getMessage());
        }

        try {
            context.getResponse().getWriter().write(JSONObject.toJSONString(result));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
