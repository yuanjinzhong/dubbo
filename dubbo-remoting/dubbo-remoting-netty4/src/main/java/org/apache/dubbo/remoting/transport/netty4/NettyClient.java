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
package org.apache.dubbo.remoting.transport.netty4;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.GlobalResourceInitializer;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractClient;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslClientTlsHandler;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslContexts;
import org.apache.dubbo.remoting.utils.UrlUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_CLIENT_CONNECT_TIMEOUT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CONNECT_PROVIDER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_DISCONNECT_PROVIDER;
import static org.apache.dubbo.remoting.Constants.DEFAULT_CONNECT_TIMEOUT;
import static org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory.eventLoopGroup;
import static org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory.socketChannelClass;

/**
 * NettyClient.
 */
public class NettyClient extends AbstractClient {

    private static final String SOCKS_PROXY_HOST = "socksProxyHost";

    private static final String SOCKS_PROXY_PORT = "socksProxyPort";

    private static final String DEFAULT_SOCKS_PROXY_PORT = "1080";

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NettyClient.class);

    /**
     * netty client bootstrap
     */
    private static final GlobalResourceInitializer<EventLoopGroup> EVENT_LOOP_GROUP = new GlobalResourceInitializer<>(() ->
        eventLoopGroup(Constants.DEFAULT_IO_THREADS, "NettyClientWorker"),
        EventExecutorGroup::shutdownGracefully);

    private Bootstrap bootstrap;

    /**
     * current channel. Each successful invocation of {@link NettyClient#doConnect()} will
     * replace this with new channel and close old channel.
     * <b>volatile, please copy reference to use.</b>
     */
    private volatile Channel channel;

    /**
     * The constructor of NettyClient.
     * It wil init and start netty.
     */
    public NettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
        // you can customize name and type of client thread pool by THREAD_NAME_KEY and THREADPOOL_KEY in CommonConstants.
        // the handler will be wrapped: MultiMessageHandler->HeartbeatHandler->handler
        super(url, wrapChannelHandler(url, handler));
    }

    /**
     * Init bootstrap
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        final NettyClientHandler nettyClientHandler = createNettyClientHandler();
        bootstrap = new Bootstrap();
        initBootstrap(nettyClientHandler);
    }

    protected NettyClientHandler createNettyClientHandler() {
        return new NettyClientHandler(getUrl(), this);
    }

    protected void initBootstrap(NettyClientHandler nettyClientHandler) {
        bootstrap.group(EVENT_LOOP_GROUP.get())
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
            .channel(socketChannelClass());

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(DEFAULT_CONNECT_TIMEOUT, getConnectTimeout()));
        SslContext sslContext = SslContexts.buildClientSslContext(getUrl());
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                int heartbeatInterval = UrlUtils.getHeartbeat(getUrl());

                if (sslContext != null) {
                    ch.pipeline().addLast("negotiation", new SslClientTlsHandler(sslContext));
                }

                /***这边的channelHandle全是可以共享的*/
//                LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);

                NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyClient.this);
                /***入栈&出栈日志记录*/
                //ch.pipeline().addLast(LOGGING_HANDLER);//为了测试
                ch.pipeline().addLast("logging",new LoggingHandler(LogLevel.DEBUG))//for debug
                    .addLast("decoder", adapter.getDecoder())
                    .addLast("encoder", adapter.getEncoder())
                    .addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS))
                    .addLast("handler", nettyClientHandler);

                String socksProxyHost = ConfigurationUtils.getProperty(getUrl().getOrDefaultApplicationModel(), SOCKS_PROXY_HOST);
                if (socksProxyHost != null && !isFilteredAddress(getUrl().getHost())) {
                    int socksProxyPort = Integer.parseInt(ConfigurationUtils.getProperty(getUrl().getOrDefaultApplicationModel(), SOCKS_PROXY_PORT, DEFAULT_SOCKS_PROXY_PORT));
                    Socks5ProxyHandler socks5ProxyHandler = new Socks5ProxyHandler(new InetSocketAddress(socksProxyHost, socksProxyPort));
                    ch.pipeline().addFirst(socks5ProxyHandler);
                }
            }
        });
    }

    private boolean isFilteredAddress(String host) {
        // filter local address
        return StringUtils.isEquals(NetUtils.getLocalHost(), host) || NetUtils.isLocalHost(host);
    }

    @Override
    protected void doConnect() throws Throwable {
        try {
            String ipv6Address = NetUtils.getLocalHostV6();
            InetSocketAddress connectAddress;
            //first try ipv6 address
            if (ipv6Address != null && getUrl().getParameter(CommonConstants.IPV6_KEY) != null) {
                connectAddress = new InetSocketAddress(getUrl().getParameter(CommonConstants.IPV6_KEY), getUrl().getPort());
                try {
                    doConnect(connectAddress);
                    return;
                } catch (Throwable throwable) {
                    //ignore
                }
            }

            connectAddress = getConnectAddress();
            doConnect(connectAddress);
        } finally {
            // just add new valid channel to NettyChannel's cache
            if (!isConnected()) {
                //future.cancel(true);
            }
        }
    }

    private void doConnect(InetSocketAddress serverAddress) throws RemotingException {
        long start = System.currentTimeMillis();
        ChannelFuture future = bootstrap.connect(serverAddress);
        try {
            boolean ret = future.awaitUninterruptibly(getConnectTimeout(), MILLISECONDS);

            if (ret && future.isSuccess()) {
                Channel newChannel = future.channel();
                try {
                    // Close old channel
                    // copy reference
                    Channel oldChannel = NettyClient.this.channel;
                    if (oldChannel != null) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + newChannel);
                            }
                            oldChannel.close();
                        } finally {
                            NettyChannel.removeChannelIfDisconnected(oldChannel);
                        }
                    }
                } finally {
                    if (NettyClient.this.isClosed()) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close new netty channel " + newChannel + ", because the client closed.");
                            }
                            newChannel.close();
                        } finally {
                            NettyClient.this.channel = null;
                            NettyChannel.removeChannelIfDisconnected(newChannel);
                        }
                    } else {
                        NettyClient.this.channel = newChannel;
                    }
                }
            } else if (future.cause() != null) {

                Throwable cause = future.cause();

                // 6-1 Failed to connect to provider server by other reason.

                RemotingException remotingException = new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server "
                    + serverAddress + ", error message is:" + cause.getMessage(), cause);

                logger.error(TRANSPORT_FAILED_CONNECT_PROVIDER, "network disconnected", "",
                    "Failed to connect to provider server by other reason.", cause);

                throw remotingException;

            } else {

                // 6-2 Client-side timeout

                RemotingException remotingException = new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server "
                    + serverAddress + " client-side timeout "
                    + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                    + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion());

                logger.error(TRANSPORT_CLIENT_CONNECT_TIMEOUT, "provider crash", "",
                    "Client-side timeout.", remotingException);

                throw remotingException;
            }
        } finally {
            // just add new valid channel to NettyChannel's cache
            if (!isConnected()) {
                //future.cancel(true);
            }
        }
    }

    @Override
    protected void doDisConnect() throws Throwable {
        try {
            NettyChannel.removeChannelIfDisconnected(channel);
        } catch (Throwable t) {
            logger.warn(TRANSPORT_FAILED_DISCONNECT_PROVIDER, "", "", t.getMessage());
        }
    }

    @Override
    protected void doClose() throws Throwable {
        // can't shut down nioEventLoopGroup because the method will be invoked when closing one channel but not a client,
        // but when and how to close the nioEventLoopGroup ?
        // nioEventLoopGroup.shutdownGracefully();
    }

    @Override
    protected org.apache.dubbo.remoting.Channel getChannel() {
        Channel c = channel;
        if (c == null) {
            return null;
        }
        return NettyChannel.getOrAddChannel(c, getUrl(), this);
    }

    Channel getNettyChannel() {
        return channel;
    }

    @Override
    public boolean canHandleIdle() {
        return true;
    }

    protected EventLoopGroup getEventLoopGroup() {
        return EVENT_LOOP_GROUP.get();
    }

    protected Bootstrap getBootstrap() {
        return bootstrap;
    }
}
