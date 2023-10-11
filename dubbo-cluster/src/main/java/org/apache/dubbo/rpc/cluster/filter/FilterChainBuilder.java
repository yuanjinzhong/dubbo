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
package org.apache.dubbo.rpc.cluster.filter;

import org.apache.dubbo.common.Experimental;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvocationProfilerUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_EXECUTE_FILTER_EXCEPTION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.extension.ExtensionScope.APPLICATION;

@SPI(value = "default", scope = APPLICATION)
public interface FilterChainBuilder {
    /**
     * build consumer/provider filter chain
     */
    <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group);

    /**
     * build consumer cluster filter chain
     */
    <T> ClusterInvoker<T> buildClusterInvokerChain(final ClusterInvoker<T> invoker, String key, String group);

    /**
     * Works on provider side
     *
     * @param <T>
     * @param <TYPE>
     */
    class FilterChainNode<T, TYPE extends Invoker<T>, FILTER extends BaseFilter> implements Invoker<T> {
        TYPE originalInvoker;
        Invoker<T> nextNode;
        FILTER filter;

        public FilterChainNode(TYPE originalInvoker, Invoker<T> nextNode, FILTER filter) {
            this.originalInvoker = originalInvoker;
            this.nextNode = nextNode;
            this.filter = filter;
        }

        public TYPE getOriginalInvoker() {
            return originalInvoker;
        }

        @Override
        public Class<T> getInterface() {
            return originalInvoker.getInterface();
        }

        @Override
        public URL getUrl() {
            return originalInvoker.getUrl();
        }

        @Override
        public boolean isAvailable() {
            return originalInvoker.isAvailable();
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            Result asyncResult;
            try {
                InvocationProfilerUtils.enterDetailProfiler(invocation, () -> "Filter " + filter.getClass().getName() + " invoke.");
                asyncResult = filter.invoke(nextNode, invocation);
            } catch (Exception e) {
                InvocationProfilerUtils.releaseDetailProfiler(invocation);
                if (filter instanceof ListenableFilter) {
                    ListenableFilter listenableFilter = ((ListenableFilter) filter);
                    try {
                        Filter.Listener listener = listenableFilter.listener(invocation);
                        if (listener != null) {
                            listener.onError(e, originalInvoker, invocation);
                        }
                    } finally {
                        listenableFilter.removeListener(invocation);
                    }
                } else if (filter instanceof FILTER.Listener) {
                    FILTER.Listener listener = (FILTER.Listener) filter;
                    listener.onError(e, originalInvoker, invocation);
                }
                throw e;
            } finally {

            }
            return asyncResult.whenCompleteWithContext((r, t) -> {
                InvocationProfilerUtils.releaseDetailProfiler(invocation);
                if (filter instanceof ListenableFilter) {
                    ListenableFilter listenableFilter = ((ListenableFilter) filter);
                    Filter.Listener listener = listenableFilter.listener(invocation);
                    try {
                        if (listener != null) {
                            if (t == null) {
                                listener.onResponse(r, originalInvoker, invocation);
                            } else {
                                listener.onError(t, originalInvoker, invocation);
                            }
                        }
                    } finally {
                        listenableFilter.removeListener(invocation);
                    }
                } else if (filter instanceof FILTER.Listener) {
                    FILTER.Listener listener = (FILTER.Listener) filter;
                    if (t == null) {
                        listener.onResponse(r, originalInvoker, invocation);
                    } else {
                        listener.onError(t, originalInvoker, invocation);
                    }
                }
            });
        }

        @Override
        public void destroy() {
            originalInvoker.destroy();
        }

        @Override
        public String toString() {
            return originalInvoker.toString();
        }
    }

    /**
     * Works on consumer side
     *
     * @param <T>
     * @param <TYPE>
     */
    class ClusterFilterChainNode<T, TYPE extends ClusterInvoker<T>, FILTER extends BaseFilter>
        extends FilterChainNode<T, TYPE, FILTER> implements ClusterInvoker<T> {
        public ClusterFilterChainNode(TYPE originalInvoker, Invoker<T> nextNode, FILTER filter) {
            super(originalInvoker, nextNode, filter);
        }


        @Override
        public URL getRegistryUrl() {
            return getOriginalInvoker().getRegistryUrl();
        }

        @Override
        public Directory<T> getDirectory() {
            return getOriginalInvoker().getDirectory();
        }

        @Override
        public boolean isDestroyed() {
            return getOriginalInvoker().isDestroyed();
        }
    }

    class CallbackRegistrationInvoker<T, FILTER extends BaseFilter> implements Invoker<T> {
        private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(CallbackRegistrationInvoker.class);
        final Invoker<T> filterInvoker;
        final List<FILTER> filters;

        public CallbackRegistrationInvoker(Invoker<T> filterInvoker, List<FILTER> filters) {
            this.filterInvoker = filterInvoker;
            this.filters = filters;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            // filterInvoker 是CopyOfFilterChainNode 链表
            Result asyncResult = filterInvoker.invoke(invocation);
            asyncResult.whenCompleteWithContext((r, t) -> {
                RuntimeException filterRuntimeException = null;
                /**
                 * 为了走org.apache.dubbo.rpc.BaseFilter.Listener的回调
                 */
                for (int i = filters.size() - 1; i >= 0; i--) {
                    FILTER filter = filters.get(i);
                    try {
                        InvocationProfilerUtils.releaseDetailProfiler(invocation);
                        if (filter instanceof ListenableFilter) {
                            ListenableFilter listenableFilter = ((ListenableFilter) filter);
                            Filter.Listener listener = listenableFilter.listener(invocation);
                            try {
                                if (listener != null) {
                                    if (t == null) {
                                        listener.onResponse(r, filterInvoker, invocation);
                                    } else {
                                        listener.onError(t, filterInvoker, invocation);
                                    }
                                }
                            } finally {
                                listenableFilter.removeListener(invocation);
                            }
                        } else if (filter instanceof FILTER.Listener) {
                            FILTER.Listener listener = (FILTER.Listener) filter;
                            if (t == null) {
                                listener.onResponse(r, filterInvoker, invocation);
                            } else {
                                listener.onError(t, filterInvoker, invocation);
                            }
                        }
                    } catch (RuntimeException runtimeException) {
                        LOGGER.error(CLUSTER_EXECUTE_FILTER_EXCEPTION, "the custom filter is abnormal", "", String.format("Exception occurred while executing the %s filter named %s.", i, filter.getClass().getSimpleName()));
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(String.format("Whole filter list is: %s", filters.stream().map(tmpFilter -> tmpFilter.getClass().getSimpleName()).collect(Collectors.toList())));
                        }
                        filterRuntimeException = runtimeException;
                        t = runtimeException;
                    }
                }
                if (filterRuntimeException != null) {
                    throw filterRuntimeException;
                }
            });

            return asyncResult;
        }

        public Invoker<T> getFilterInvoker() {
            return filterInvoker;
        }

        @Override
        public Class<T> getInterface() {
            return filterInvoker.getInterface();
        }

        @Override
        public URL getUrl() {
            return filterInvoker.getUrl();
        }

        @Override
        public boolean isAvailable() {
            return filterInvoker.isAvailable();
        }

        @Override
        public void destroy() {
            filterInvoker.destroy();
        }
    }

    class ClusterCallbackRegistrationInvoker<T, FILTER extends BaseFilter> extends CallbackRegistrationInvoker<T, FILTER>
        implements ClusterInvoker<T> {
        private ClusterInvoker<T> originalInvoker;

        public ClusterCallbackRegistrationInvoker(ClusterInvoker<T> originalInvoker, Invoker<T> filterInvoker, List<FILTER> filters) {
            super(filterInvoker, filters);
            this.originalInvoker = originalInvoker;
        }

        public ClusterInvoker<T> getOriginalInvoker() {
            return originalInvoker;
        }

        @Override
        public URL getRegistryUrl() {
            return getOriginalInvoker().getRegistryUrl();
        }

        @Override
        public Directory<T> getDirectory() {
            return getOriginalInvoker().getDirectory();
        }

        @Override
        public boolean isDestroyed() {
            return getOriginalInvoker().isDestroyed();
        }
    }


    @Experimental("Works for the same purpose as FilterChainNode, replace FilterChainNode with this one when proved stable enough")
    class CopyOfFilterChainNode<T, TYPE extends Invoker<T>, FILTER extends BaseFilter> implements Invoker<T> {
        private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(CopyOfFilterChainNode.class);
        TYPE originalInvoker;
        Invoker<T> nextNode; // 这个也是CopyOfFilterChainNode
        FILTER filter;

        public CopyOfFilterChainNode(TYPE originalInvoker, Invoker<T> nextNode, FILTER filter) {
            this.originalInvoker = originalInvoker;
            this.nextNode = nextNode;
            this.filter = filter;
        }

        public TYPE getOriginalInvoker() {
            return originalInvoker;
        }

        @Override
        public Class<T> getInterface() {
            return originalInvoker.getInterface();
        }

        @Override
        public URL getUrl() {
            return originalInvoker.getUrl();
        }

        @Override
        public boolean isAvailable() {
            return originalInvoker.isAvailable();
        }

        /**
         * <p>首先每个{@link CopyOfFilterChainNode}对象内部都有维护各自的{@link filter}、{@link  nextNode}
         *
         * <p>在{@link CopyOfFilterChainNode}内部的{@link invoke} 方法中，委托调用{@link BaseFilter#invoke(Invoker, Invocation)}，
         *
         * <p>该方法规定:Always call invoker.invoke() in the implementation to hand over （传递）the request to the next filter node.
         *
         * <p>方法入参的{@link invoker}是这里的{@link nextNode} 也就是 {@link CopyOfFilterChainNode}对象，
         *
         * <p>调用 invoker.invoke()时，等价于递归调用当前方法{FilterChainBuilder.CopyOfFilterChainNode#invoke(Invocation)}
         *
         * <p>当前{@link CopyOfFilterChainNode} 是个链表，持有下一个{@link CopyOfFilterChainNode} 用{@linK nextNode}属性表示
         * <p>
         * <p>链表的最后一个节点的俩个属性分别是：
         * <li>{@link nextNode}:真正的invoker,等于originalInvoker属性</li>
         * <li>{@link filter}: 从扩展点中加载的排好序的filter的最后一个（因为构建链表的时候，最后一个filter和真正的invoker 被包在最内层）</li>
         *
         * <p><b>该方法最终达到的效果是: 各种{@link filter.invoke}方法先执行，再执行最后一个filter.invoke的时候，执行真正的invoker的invoke方法，从而发起RPC调用</b></p>
         *
         */
        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            Result asyncResult;
            try {
                /** 至关重要的方法
                 *  各种{@link filter.invoke}方法先执行，再执行最后一个filter.invoke的时候，执行真正的invoker的invoke方法，从而发起RPC调用
                 */
                asyncResult = filter.invoke(nextNode, invocation);
                if (!(asyncResult instanceof AsyncRpcResult)) {
                    String msg = "The result of filter invocation must be AsyncRpcResult. (If you want to recreate a result, please use AsyncRpcResult.newDefaultAsyncResult.) " +
                        "Filter class: " + filter.getClass().getName() + ". Result class: " + asyncResult.getClass().getName() + ".";
                    LOGGER.error(INTERNAL_ERROR, "", "", msg);
                    throw new RpcException(msg);
                }
            } catch (Exception e) {
                InvocationProfilerUtils.releaseDetailProfiler(invocation);
                if (filter instanceof ListenableFilter) {
                    ListenableFilter listenableFilter = ((ListenableFilter) filter);
                    try {
                        Filter.Listener listener = listenableFilter.listener(invocation);
                        if (listener != null) {
                            listener.onError(e, originalInvoker, invocation);
                        }
                    } finally {
                        listenableFilter.removeListener(invocation);
                    }
                } else if (filter instanceof FILTER.Listener) {
                    FILTER.Listener listener = (FILTER.Listener) filter;
                    listener.onError(e, originalInvoker, invocation);
                }
                throw e;
            } finally {

            }
            return asyncResult;
        }

        @Override
        public void destroy() {
            originalInvoker.destroy();
        }

        @Override
        public String toString() {
            return originalInvoker.toString();
        }
    }

    @Experimental("Works for the same purpose as ClusterFilterChainNode, replace ClusterFilterChainNode with this one when proved stable enough")
    class CopyOfClusterFilterChainNode<T, TYPE extends ClusterInvoker<T>, FILTER extends BaseFilter>
        extends CopyOfFilterChainNode<T, TYPE, FILTER> implements ClusterInvoker<T> {
        public CopyOfClusterFilterChainNode(TYPE originalInvoker, Invoker<T> nextNode, FILTER filter) {
            super(originalInvoker, nextNode, filter);
        }


        @Override
        public URL getRegistryUrl() {
            return getOriginalInvoker().getRegistryUrl();
        }

        @Override
        public Directory<T> getDirectory() {
            return getOriginalInvoker().getDirectory();
        }

        @Override
        public boolean isDestroyed() {
            return getOriginalInvoker().isDestroyed();
        }
    }
}
