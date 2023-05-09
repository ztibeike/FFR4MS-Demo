package io.ztbeike.ffr4ms.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@RestController
@RequestMapping("/serviceA")
public class ComputingController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String SERVICE_B_URL = "http://zuul-gateway-b/serviceB";

    @GetMapping("/compute/{value}")
    public String compute(@PathVariable("value") String value) {
        Random random = new Random();
        try {
            Integer val = Integer.parseInt(value);
            Thread.sleep(200 + random.nextInt(300)); // 模拟工作时间
            String result = restTemplate.postForObject(SERVICE_B_URL + "/compute", val, String.class);
            Thread.sleep(200 + random.nextInt(400)); // 模拟工作时间
            return value + "的平方为: " + result;
        } catch (NumberFormatException | InterruptedException e) {
            return "输入的值" + value + "不是整数";
        }
    }

}
