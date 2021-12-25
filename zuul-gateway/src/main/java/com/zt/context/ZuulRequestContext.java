package com.zt.context;

import com.netflix.zuul.context.RequestContext;
import lombok.experimental.UtilityClass;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;

@UtilityClass
public class ZuulRequestContext {

    public RequestContext getContext() {
        return RequestContext.getCurrentContext();
    }

    public HttpServletRequest getRequest() {
        return getContext().getRequest();
    }

    public String getRequestHeader(String header) {
        final Map<String, String> requestHeaders = getRequestHeaders();
        final String value = requestHeaders.get(header);
        return value == null ? requestHeaders.get(header.toLowerCase()) : value;
    }

    public Map<String, String> getRequestHeaders() {
        // 先取context对象
        final Map<String, String> headers = getContext().getZuulRequestHeaders();
        final HttpServletRequest request = getRequest();
        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String key = headerNames.nextElement();
            if (!headers.containsKey(key)) {
                headers.put(key, request.getHeader(key));
            }
        }
        return headers;
    }

}
