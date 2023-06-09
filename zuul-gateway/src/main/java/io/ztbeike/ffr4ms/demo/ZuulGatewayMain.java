package io.ztbeike.ffr4ms.demo;


import io.ztbeike.ffr4ms.gateway.autoconfigure.EnableGatewayPlugin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableZuulProxy
@EnableEurekaClient
@EnableGatewayPlugin
public class ZuulGatewayMain {
    public static void main(String[] args) {
        SpringApplication.run(ZuulGatewayMain.class, args);
    }
}
