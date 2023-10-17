package org.apache.dubbo.springboot.demo.provider.filter;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

/**
 * @author: yuanjinzhong
 * @date: 2022/7/13 11:54 上午
 * @description: rpc 统一异常处理
 */
@Activate(group = {PROVIDER})
public class ExceptionFilter implements Filter , BaseFilter.Listener {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result = invoker.invoke(invocation);
//        String response="";
//        if (result.hasException()) {
//            // 是Dubbo本身的异常，直接抛出
//            if (result.getException() instanceof RpcException) {
//                return result;
//            }   else {
//                response="将异常处理成正常响应";
//            }
//           result.setException(null);
//           result.setValue(response);
//        }
        return result;
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        System.out.println("provider正常响应");
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        System.out.println("provider发生异常");
    }



}
