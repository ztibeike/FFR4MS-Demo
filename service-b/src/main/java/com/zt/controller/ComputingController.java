package com.zt.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/serviceB")
public class ComputingController {

    @PostMapping("/compute")
    public String compute(@RequestBody String value) {
        Integer val = Integer.parseInt(value);
        int result = val * val;
        return Integer.toString(result);
    }
}
