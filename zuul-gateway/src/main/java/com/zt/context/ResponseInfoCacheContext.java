package com.zt.context;

import com.alibaba.fastjson.JSONObject;
import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;
import com.zt.cache.ZuulMessageCache;
import com.zt.constant.ZuulLinkTraceConstant;
import com.zt.model.MicroServiceResponseModel;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description zuul网关响应信息缓存上下文
 */
@Component
public class ResponseInfoCacheContext {

    @Autowired
    private ZuulMessageCache<String> messageCache;

    /**
     * 生成缓存 key
     *
     * @param serviceName
     * @param uri
     * @return
     */
    private String generateCacheKey(String serviceName, String uri) {
        // serviceInstanceId:tId:serviceName:uri
        Assert.hasText(serviceName, "service name not be empty");
        Assert.hasText(uri, "request uri not be empty");
        return new StringBuilder(ZuulRequestContext.getRequestHeader(ZuulLinkTraceConstant.SERVICE_NAME_HEADER))
                .append(":")
                .append(ZuulRequestContext.getRequestHeader(ZuulLinkTraceConstant.TRACE_ID_HEADER))
                .append(":")
                .append(serviceName)
                .append(":")
                .append(uri)
                .toString();
    }

    /**
     * 构建缓存信息
     *
     * @param model
     * @return
     */
    private String buildCacheInfo(MicroServiceResponseModel model) {
        Assert.notNull(model, "response model not be null");
        return JSONObject.toJSONString(model);
    }

    /**
     * 请求上下文中对响应进行缓存
     * 需在 postFilter 中调用
     *
     * @throws Exception
     */
    public void set() throws Exception {
        if (StringUtils.isEmpty(ZuulRequestContext.getRequestHeader(ZuulLinkTraceConstant.SERVICE_NAME_HEADER))) {
            // 无服务名称，跳过报文缓存
            return;
        }
        RequestContext context = ZuulRequestContext.getContext();

        List<Pair<String, String>> list = context.getZuulResponseHeaders();
        if (!CollectionUtils.isEmpty(list)) {
            for (Pair<String, String> pair : list) {
                if (ZuulLinkTraceConstant.ZUUL_RESPONSE_CACHE_BACK.equals(pair.first())
                        && Boolean.TRUE.toString().equals(pair.second())) {
                    // zuul缓存响应，直接返回
                    return;
                }
            }
        }

        HttpServletResponse response = context.getResponse();

        Map<String, List<String>> headers = new HashMap<>();

        // zuul response
        Object zuulResponse = context.get("zuulResponse");

        // service instance response body
        String body = null;

        // service name
        final Object serviceId = context.get("serviceId");
        String serviceName = serviceId == null ? null : serviceId.toString();
        if (zuulResponse != null) {
            // get service response headers
            RibbonHttpResponse resp = (RibbonHttpResponse) zuulResponse;
            body = IOUtils.toString(resp.getBody(), response.getCharacterEncoding());
            context.setResponseBody(body);

            HttpHeaders respHeaders = resp.getHeaders();

            headers.putAll(respHeaders);
        }

        URI uri = null;
        Object ribbonResponse = context.get("ribbonResponse");
        if (ribbonResponse != null) {
            RibbonApacheHttpResponse httpResponse = (RibbonApacheHttpResponse) ribbonResponse;
            uri = httpResponse.getRequestedURI();
        }

        MicroServiceResponseModel model = new MicroServiceResponseModel();
        model.setTranceId(ZuulRequestContext.getRequestHeader(ZuulLinkTraceConstant.TRACE_ID_HEADER));
        model.setResponseCode(response.getStatus());
        model.setHeaders(headers);
        model.setBody(body);
        model.setServiceName(serviceName);
        model.setRequestUri(context.get("requestURI").toString());

        if (uri != null) {
            model.setHost(uri.getHost());
            model.setPort(uri.getPort());
        }

        this.messageCache.set(generateCacheKey(model.getServiceName(), model.getRequestUri()), buildCacheInfo(model));
    }

    /**
     * 获取服务调用响应缓存
     * 需在 routeFilter 之后调用
     *
     * @return
     */
    public MicroServiceResponseModel get() {
        RequestContext context = ZuulRequestContext.getContext();
        final Object serviceId = context.get("serviceId");
        String serviceName = serviceId == null ? null : serviceId.toString();

        final Object requestURI = context.get("requestURI");
        String uri = requestURI == null ? null : requestURI.toString();

        String cache = this.messageCache.get(generateCacheKey(serviceName, uri));
        return StringUtils.isEmpty(cache) ? null : JSONObject.parseObject(cache, MicroServiceResponseModel.class);
    }

}
