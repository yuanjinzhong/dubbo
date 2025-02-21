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
package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * InvokerHandler
 */
public class InvokerInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);

    private final Invoker<?> invoker;

    private final ServiceModel serviceModel;

    private final String protocolServiceKey;

    public InvokerInvocationHandler(Invoker<?> handler) {
        this.invoker = handler;
        URL url = invoker.getUrl();
        this.protocolServiceKey = url.getProtocolServiceKey();
        this.serviceModel = url.getServiceModel();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(invoker, args);
        }
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return invoker.toString();
            } else if ("$destroy".equals(methodName)) {
                invoker.destroy();
                return null;
            } else if ("hashCode".equals(methodName)) {
                return invoker.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return invoker.equals(args[0]);
        }
        RpcInvocation rpcInvocation = new RpcInvocation(serviceModel, method.getName(), invoker.getInterface().getName(), protocolServiceKey, method.getParameterTypes(), args);

        //consumer发送请求，provider接收请求都是走这里， 只不过不同模式有设置不同的参数
        if (serviceModel instanceof ConsumerModel) {
            rpcInvocation.put(Constants.CONSUMER_MODEL, serviceModel);
            rpcInvocation.put(Constants.METHOD_MODEL, ((ConsumerModel) serviceModel).getMethodModel(method));
        }// todo 真正的发起RPC调用
        return InvocationUtil.invoke(invoker, rpcInvocation);
    }
}
