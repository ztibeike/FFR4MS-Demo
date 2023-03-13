package io.ztbeike.ffr4ms.demo;


import io.ztbeike.ffr4ms.registry.EnableRegistryPlugin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
@EnableRegistryPlugin
public class EurekaRegistryMain {
    public static void main(String[] args) {
        SpringApplication.run(EurekaRegistryMain.class, args);
    }
}
