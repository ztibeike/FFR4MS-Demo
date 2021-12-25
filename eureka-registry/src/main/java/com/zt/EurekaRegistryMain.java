package com.zt;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaRegistryMain {
    public static void main(String[] args) {
        SpringApplication.run(EurekaRegistryMain.class, args);
    }
}
