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
package org.apache.dubbo.remoting;

import org.apache.dubbo.common.timer.HashedWheelTimer;

/**
 * Indicate whether the implementation (for both server and client) has the ability to sense and handle idle connection.
 * If the server has the ability to handle idle connection, it should close the connection when it happens, and if
 * the client has the ability to handle idle connection, it should send the heartbeat to the server.
 * todo 空闲链接感知接口， 方法返回true,则 server、client端需要能够处理空闲链接
 */
public interface IdleSensible {
    /**
     * Whether the implementation can sense and handle the idle connection. By default, it's false, the implementation
     * relies on dedicated timer to take care of idle connection.
     *
     * @return whether it has the ability to handle idle connection //todo true/false 返回当前 服务端或者客户端 是否有能力处理空闲链接; 默认情况下，两个端是通过定时任务处理空闲链接的
     *
     * @see HashedWheelTimer
     * @see HashedWheelTimer.HashedWheelTimeout
     */
    default boolean canHandleIdle() {
        return false;
    }
}
