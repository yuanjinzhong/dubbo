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
package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

/**
 * Cluster. (SPI, Singleton, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Computer_cluster">Cluster</a>
 * <a href="http://en.wikipedia.org/wiki/Fault-tolerant_system">Fault-Tolerant</a>
 *
 * <p>Cluster将Directory中的多个Invoker伪装成一个 Invoker，对上层透明，伪装过程包含了容错逻辑，调用失败后，重试另一个；</p>
 *
 * <p>这表示一个集群，实现类有， 失效转移集群，快速失败集群，FailOver FailFast， FailSafe，FailBack</p>
 *
 * <a href="https://dubbo.apache.org/zh-cn/overview/mannual/java-sdk/advanced-features-and-usage/service/fault-tolerent-strategy/#%E7%89%B9%E6%80%A7%E8%AF%B4%E6%98%8E">dubbo 集群的说明：cluster</a>
 *
 */
@SPI(Cluster.DEFAULT)
public interface Cluster {

    String DEFAULT = "failover";

    /**
     * Merge the directory invokers to a virtual invoker.
     * <p>
     * <p>将多个物理invoker(directory就表示invoker集合)包装成一个虚拟的invoker</p>
     * <p>
     * @param <T>
     * @param directory
     * @return cluster invoker
     * @throws RpcException
     */
    @Adaptive
    <T> Invoker<T> join(Directory<T> directory, boolean buildFilterChain) throws RpcException;

    static Cluster getCluster(ScopeModel scopeModel, String name) {
        return getCluster(scopeModel, name, true);
    }

    static Cluster getCluster(ScopeModel scopeModel, String name, boolean wrap) {
        if (StringUtils.isEmpty(name)) {
            name = Cluster.DEFAULT;
        }
        return ScopeModelUtil.getApplicationModel(scopeModel).getExtensionLoader(Cluster.class).getExtension(name, wrap);
    }
}
