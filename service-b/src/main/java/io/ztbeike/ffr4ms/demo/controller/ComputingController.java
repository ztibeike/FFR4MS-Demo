package io.ztbeike.ffr4ms.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/serviceB")
public class ComputingController {

    @PostMapping("/compute")
    public String compute(@RequestBody String value) {
        Integer val = Integer.parseInt(value);
        int result = val * val;
        Random random = new Random();
        try {
            Thread.sleep(200 + random.nextInt(500)); // 模拟工作时间
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Integer.toString(result);
    }
}
