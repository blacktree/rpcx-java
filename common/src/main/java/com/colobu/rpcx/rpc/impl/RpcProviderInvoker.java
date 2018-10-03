package com.colobu.rpcx.rpc.impl;

import com.colobu.rpcx.rpc.Invoker;
import com.colobu.rpcx.rpc.Result;
import com.colobu.rpcx.rpc.RpcException;
import com.colobu.rpcx.rpc.URL;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * @author goodjava@qq.com
 */
public class RpcProviderInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(RpcProviderInvoker.class);

    private static final boolean useMethodAccess = true;

    /**
     * 如果是基于ioc容器的,需要提供获取bean的function
     */
    private Function<Class, Object> getBeanFunc;

    private URL url;

    private Class clazz;

    private Method method;

    private MethodAccess methodAccess;

    public RpcProviderInvoker(Function<Class, Object> getBeanFunc) {
        this.getBeanFunc = getBeanFunc;
    }

    @Override
    public Class<T> getInterface() {
        return clazz;
    }


    @Override
    public Result invoke(RpcInvocation invocation) {
        try {
            Object obj = null;
            Result rpcResult = new RpcResult();
            //使用容器
            if (null != this.getBeanFunc) {
                Object b = getBeanFunc.apply(clazz);
                if (useMethodAccess) {
                    obj = methodAccess.invoke(b, invocation.getMethodName(),invocation.getArguments());
                } else {
                    obj = this.method.invoke(b, invocation.getArguments());
                }
            } else {//不使用容器
                if (useMethodAccess) {
                   obj = methodAccess.invoke(clazz.newInstance(), invocation.getMethodName(), invocation.getArguments());
                } else {
                    obj = this.method.invoke(clazz.newInstance(), invocation.getArguments());
                }
            }
            rpcResult.setValue(obj);
            return rpcResult;
        } catch (Throwable throwable) {
            String message = "";
            if (throwable.getCause() != null) {
                message = throwable.getCause().getMessage();
            }
            throw new RpcException(message, throwable, "-2");
        }
    }

    @Override
    public URL getUrl() {
        return this.url;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public Method getMethod() {
        return this.method;
    }

    @Override
    public void setInterface(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void setMethodAccess(MethodAccess methodAccess) {
        this.methodAccess = methodAccess;
    }
}
