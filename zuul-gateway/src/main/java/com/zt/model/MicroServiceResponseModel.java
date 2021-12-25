package com.zt.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @Description 微服务响应实体
 */
@Getter
@Setter
public class MicroServiceResponseModel {

    /**
     * 追踪Id
     */
    private String tranceId;

    /**
     * 响应码
     */
    private Integer responseCode;

    /**
     * 响应头
     */
    private Map<String, List<String>> headers;

    /**
     * 响应体
     */
    private String body;

    /***
     * 服务实例主机
     */
    private String host;

    /**
     * 服务实例服务端口
     */
    private Integer port;

    /**
     * 服务实例名称（spring.application.name）
     */
    private String serviceName;

    /**
     * 请求uri
     */
    private String requestUri;

}
