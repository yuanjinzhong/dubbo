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
package org.apache.dubbo.common.extension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *  <p>dubbo 的 spi 中，判断当前"扩展实现类"是否是 "扩展接口的包装实现类" 不是通过 @Wrapper接口来的
 *  <p>而是通过 反射判断"该包装实现类是否包含 仅有一个扩展接口作为参数的构造函数"来判断的
 *
 *  <p>@Wrapper注解是在 知道了一堆扩展实现类之后，控制组装（构造）包装类实现类的策略（通过 matches，mismatches 来看是否匹配ExtentionLoader查找的spi的名字）
 *
 *
 *  <p>构造包装类的时候可以无限套娃： 包装类A--持有--包装类B--持有--包装类C---持有--扩展点默认接口
 *  <p>                          DemoWrapper1-> DemoWrapper2->DemoImpl
 *
 *
 *
 * The annotated class will only work as a wrapper when the condition matches.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Wrapper {

    /**
     * the extension names that need to be wrapped.
     * default is matching when this array is empty.
     */
    String[] matches() default {};

    /**
     * the extension names that need to be excluded.
     */
    String[] mismatches() default {};

    /**
     * absolute ordering, optional
     * ascending order, smaller values will be in the front of the list.
     * @return
     */
    int order() default 0;
}
