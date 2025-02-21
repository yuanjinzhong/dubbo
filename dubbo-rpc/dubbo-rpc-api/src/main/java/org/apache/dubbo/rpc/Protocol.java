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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

import java.util.Collections;
import java.util.List;

/**
 * RPC Protocol extension interface, which encapsulates(封装) the details of remote invocation. <br /><br />
 *
 * <p>Conventions（约定）:
 *
 * <li>
 *     When user invokes the 'invoke()' method in object that the method 'refer()' returns,
 *     the protocol needs to execute the 'invoke()' method of Invoker object that received by 'export()' method,
 *     which should have the same URL.
 * </li>
 *
 * <li>
 *     Invoker that returned by 'refer()' is implemented by the protocol. The remote invocation request should be sent by that Invoker.
 * </li>
 *
 * <li>
 *     The invoker that 'export()' receives will be implemented by framework. Protocol implementation should not care with that.
 * </li>
 *
 * <p>Attentions:
 *
 * <li>
 *     The Protocol implementation does not care the transparent（透明的） proxy. The invoker will be converted to business interface by other layer.
 *
 *                                                                               这个invoker是框架传入，自定义协议的时候不需要关心这个参数
 * </li>
 *
 * <li>
 *     The protocol doesn't need to be backed by TCP connection. It can also be backed by file sharing or inter-process communication.
 *     协议层属于上层抽象，不指定具体的通信方式
 * </li>
 *
 * (API/SPI, Singleton, ThreadSafe)
 */
@SPI(value = "dubbo", scope = ExtensionScope.FRAMEWORK)
public interface Protocol {

    /**
     * Get default port when user doesn't config the port.
     *
     * @return default port
     */
    int getDefaultPort();

    /**
     * 将一个Invoker暴露出去，export()方法实现需要是幂等的，
     * 即同一个服务暴露多次和暴露一次的效果是相同的
     *
     * Export service for remote invocation: <br>
     * 1. Protocol should record request source address after receive a request:
     * RpcContext.getServerAttachment().setRemoteAddress();<br>
     * 2. export() must be idempotent, that is, there's no difference between invoking once and invoking twice when
     * export the same URL<br>
     * 3. Invoker instance is passed in by the framework, protocol needs not to care <br> 这个invoker是框架传入，自定义协议的时候不需要关系这个参数
     *
     * @param <T>     Service type
     * @param invoker Service invoker
     * @return exporter reference for exported service, useful for unexport the service later
     * @throws RpcException thrown when error occurs during export the service, for example: port is occupied
     */
    @Adaptive
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;//todo 这个invoker是框架传入，自定义协议的时候不需要关系这个参数

    /**
     * 引用一个Invoker，refer()方法会根据参数返回一个Invoker对象，
     *
     * Consumer端可以通过这个Invoker请求到Provider端的服务
     *
     * Refer a remote service: <br>
     * 1. When user calls `invoke()` method of `Invoker` object which's returned from `refer()` call, the protocol
     * needs to correspondingly（相应的） execute `invoke()` method of `Invoker` object <br>
     * 2. It's protocol's responsibility to implement `Invoker` which's returned from `refer()`. Generally speaking,
     * protocol sends remote request in the `Invoker` implementation. <br>
     *
     * <p>todo 协议需要实现invoker的实现，通常来说，发送远程请求的实现在invoker里面</p>
     *
     * 3. When there's check=false set in URL, the implementation must not throw exception but try to recover when
     * connection fails.
     *
     * <p>todo  url里面有check=false时，invoker 必须不能抛出异常， 且当远程链接恢复的时候需要尝试恢复调用</p>
     *
     * todo consumer 通过 {@link refer} 方法，得到一个Invoker，consumer端通过这个Invoker发起远程调用
     *
     * @param <T>  Service type
     * @param type Service class
     * @param url  URL address for the remote service
     * @return invoker service's local proxy
     * @throws RpcException when there's any error while connecting to the service provider
     */
    @Adaptive
    <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;

    /**
     * Destroy protocol: <br>
     * 1. Cancel all services this protocol exports and refers <br>
     * 2. Release all occupied resources, for example: connection, port, etc. <br>
     * 3. Protocol can continue to export and refer new service even after it's destroyed.
     */
    void destroy();

    /**
     * Get all servers serving this protocol
     *
     * @return
     */
    default List<ProtocolServer> getServers() {
        return Collections.emptyList();
    }

}
