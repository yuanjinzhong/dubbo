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
package org.apache.dubbo.config.spring.context.annotation;

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.config.spring.context.DubboConfigApplicationListener;
import org.apache.dubbo.config.spring.context.DubboDeployApplicationListener;
import org.apache.dubbo.config.spring.context.DubboSpringInitializer;

import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;

/**
 * codex dubbo的基架类，导入bean工厂
 *
 * Dubbo {@link AbstractConfig Config} {@link ImportBeanDefinitionRegistrar register}, which order can be configured
 *
 * @see EnableDubboConfig
 * @see DubboConfigConfiguration
 * @see Ordered
 * @since 2.5.8
 */
public class DubboConfigConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // initialize dubbo beans
        DubboSpringInitializer.initialize(registry);

        /**
         * 通过{@link EnableDubboConfig } 注解来的， 这个方法内部 会注册 几个核心的基础架构类到 beanDefinition中，
         * @see  DubboBeanUtils#registerCommonBeans(BeanDefinitionRegistry)
         *
         * 包括重量级的：{@link ReferenceAnnotationBeanPostProcessor} {@link DubboDeployApplicationListener} {@link  DubboConfigApplicationListener}
         *
         */
    }
}
