package org.apache.dubbo.springboot.demo.consumer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.service.EchoService;
import org.apache.dubbo.springboot.demo.DemoService;

/**
 * 通过{@link org.apache.dubbo.common.bytecode.Proxy#getProxy(java.lang.Class[]) }生成的代理类， 表示一个 provider端 服务的引用
 * {@link JavassistProxyFactory} 和{@link org.apache.dubbo.config.spring.ReferenceBean} 直接依赖这个
 *
 */
public class DemoServiceDubboProxy0 implements ClassGenerator.DC, DemoService, EchoService, Destroyable {
    public static Method[] methods;
    private InvocationHandler handler; // 这个实现类是：InvokerInvocationHandler

    public CompletableFuture sayHelloAsync(String string)  {
        Object[] objectArray = new Object[]{string};
        Object object = null;
        try {
            object = this.handler.invoke(this, methods[0], objectArray);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return (CompletableFuture) object;
    }

    public String sayHello(String string) {
        Object[] objectArray = new Object[]{string};
        Object object = null;
        try {
            object = this.handler.invoke(this, methods[1], objectArray);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return (String) object;
    }

    @Override
    public Object $echo(Object object) {
        Object[] objectArray = new Object[]{object};
        Object object2 = null;
        try {
            object2 = this.handler.invoke(this, methods[2], objectArray);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return object2;
    }

    @Override
    public void $destroy() {
        Object[] objectArray = new Object[]{};
        try {
            Object object = this.handler.invoke(this, methods[3], objectArray);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public DemoServiceDubboProxy0() {
    }

    public DemoServiceDubboProxy0(InvocationHandler invocationHandler) {
        this.handler = invocationHandler;
    }
}
