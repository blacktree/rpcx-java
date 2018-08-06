package com.colobu.rpcx.rpc.impl;

import com.colobu.rpcx.discovery.IServiceDiscovery;
import com.colobu.rpcx.netty.IClient;
import com.colobu.rpcx.protocol.*;
import com.colobu.rpcx.rpc.*;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author goodjava@qq.com
 */
public class RpcConsumerInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(RpcConsumerInvoker.class);

    private static final AtomicInteger seq = new AtomicInteger();

    private IClient client;

    private URL url;

    public RpcConsumerInvoker(IClient client, RpcInvocation invocation) {
        Gson gson = new Gson();
        this.client = client;
        this.url = new URL("rpcx", "", 0);
        url.setServiceInterface(invocation.getClassName() + "" + invocation.getMethodName());
        String params = Stream.of(invocation.getArguments()).map(it -> gson.toJson(it)).collect(Collectors.joining(","));
        this.url.setPath(invocation.getClassName() + "." + invocation.getMethodName() + "(" + params + ")");
    }

    @Override
    public Class<T> getInterface() {
        return null;
    }

    @Override
    public Result invoke(RpcInvocation invocation) throws RpcException {
        String className = invocation.getClassName();
        String method = invocation.getMethodName();
        RpcResult result = new RpcResult();

        Message req = new Message(className, method);
        req.setVersion((byte) 0);
        req.setMessageType(MessageType.Request);
        req.setHeartbeat(false);
        req.setOneway(false);
        req.setCompressType(CompressType.None);
        req.setSerializeType(SerializeType.SerializeNone);
        req.metadata.put("language", LanguageCode.JAVA.name());
        req.metadata.put("sendType", invocation.getSendType());
        invocation.setUrl(this.url);
        byte[] data = HessianUtils.write(invocation);
        req.payload = data;

        try {
            req.setSeq(seq.incrementAndGet());
            Message res = client.call(req, invocation.getTimeOut());
            if (res.metadata.containsKey("_rpcx_error_code")) {
                int code = Integer.parseInt(res.metadata.get("_rpcx_error_code"));
                String message = res.metadata.get("_rpcx_error_message");
                logger.warn("client call error:{}:{}", code, message);
                RpcException error = new RpcException(message, code);
                result.setThrowable(error);
            } else {
                byte[] d = res.payload;
                if (d.length > 0) {
                    Object r = HessianUtils.read(d);
                    result.setValue(r);
                }
            }
        } catch (Throwable e) {
            result.setThrowable(e);
            logger.info("client call error:{} ", e.getMessage());
        }

        logger.info("class:{} method:{} result:{} finish", className, method, new Gson().toJson(result));
        return result;
    }

    @Override
    public void setMethod(Method method) {

    }

    @Override
    public void setInterface(Class clazz) {

    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setUrl(URL url) {

    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {

    }

    @Override
    public IServiceDiscovery serviceDiscovery() {
        return this.client.getServiceDiscovery();
    }
}
