/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.springboot.demo.consumer;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.springboot.demo.DemoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE;

@SpringBootApplication
/**
 * 可以不需要下面的{@link EnableDubbo}注解，前提是yml里面含有
 *   scan:
 *     base-packages: org.apache.dubbo.springboot.demo.provider
 * 配置
 *
 * 因为DubboAutoConfiguration自动配置了
 */public class ConsumerApplication99 {

    @DubboReference
    private DemoService demoService;

    public static void main(String[] args) {
        System.setProperty(DUBBO_NETWORK_IGNORED_INTERFACE,"en6,utun5,utun4,utun3,utun2,utun1,utun0,en2,en1,en3,en4,llw0,bridge0,awdl0,ap1,en8,stf0,gif0");
        ConfigurableApplicationContext context = SpringApplication.run(ConsumerApplication99.class, args);
        ConsumerApplication99 application = context.getBean(ConsumerApplication99.class);
//        String result = application.doSayHello("world");
//        System.out.println("result: " + result);
    }

//    public String doSayHello(String name) {
//        return demoService.sayHello(name);
//    }
}
