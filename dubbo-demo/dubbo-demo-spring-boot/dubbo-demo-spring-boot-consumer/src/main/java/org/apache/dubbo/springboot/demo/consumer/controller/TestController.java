package org.apache.dubbo.springboot.demo.consumer.controller;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.springboot.demo.DemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: yuanjinzhong
 * @date: 2023/10/17 17:54
 * @description:
 */
@RestController
public class TestController {
    @DubboReference
    private DemoService demoService;


    @GetMapping("/dubbo")
    public Object test(){
        return   demoService.sayHello("world");
    }

}
