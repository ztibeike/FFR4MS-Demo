package io.ztbeike.ffr4ms.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/serviceA")
public class ComputingController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String SERVICE_B_URL = "http://zuul-gateway-b/serviceB";

    @GetMapping("/compute/{value}")
    public String compute(@PathVariable("value") String value) {
        try {
            Integer val = Integer.parseInt(value);
            String result = restTemplate.postForObject(SERVICE_B_URL + "/compute", val, String.class);
            return value + "的平方为: " + result;
        } catch (NumberFormatException e) {
            return "输入的值" + value + "不是整数";
        }
    }

}
