package com.my;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

/**
 * @author duoyian
 * @date 2026/7/20
 */
public class GatewayRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 1. 过滤心跳和无效请求
        if (request.uri().equals("/favicon.ico")) {
            return;
        }

        // 2. 路由判断
        if (request.uri().startsWith("/api/invoke")) {
            // 处理泛化调用
            handleGeneralizedInvocation(ctx, request);
        } else {
            // 处理常规 HTTP 请求
            handleNormalHttp(ctx, request);
        }
    }

    /**
     * 泛化调用处理逻辑
     */
    private void handleGeneralizedInvocation(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.method().equals(HttpMethod.POST)) {
            sendResponse(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, "Generalized invocation requires POST");
            return;
        }

        // 解析 JSON Payload
        byte[] bytes = new byte[request.content().readableBytes()];
        request.content().readBytes(bytes);

        JsonNode payload = objectMapper.readTree(bytes);
        String interfaceName = payload.get("interfaceName").asText();
        String methodName = payload.get("methodName").asText();

        // 解析参数类型和参数值
        JsonNode typesNode = payload.get("parameterTypes");
        JsonNode argsNode = payload.get("arguments");

        Class<?>[] paramTypes = new Class<?>[typesNode.size()];
        Object[] args = new Object[argsNode.size()];

        for (int i = 0; i < typesNode.size(); i++) {
            // 类型转换：基础数据类型与对象类型映射
            paramTypes[i] = mapToClass(typesNode.get(i).asText());
            args[i] = objectMapper.treeToValue(argsNode.get(i), paramTypes[i]);
        }

        // 反射加载类并执行
        // 安全提示：实际生产环境中，interfaceName必须经过白名单校验，防止任意类加载攻击
        Class<?> targetClass = Class.forName(interfaceName);
        Object targetInstance = targetClass.getDeclaredConstructor().newInstance();

        // 执行方法
        Object result = targetClass.getMethod(methodName, paramTypes).invoke(targetInstance, args);

        // 返回 JSON 结果
        String responseJson = objectMapper.writeValueAsString(result);
        sendResponse(ctx, HttpResponseStatus.OK, responseJson);
    }

    /**
     * 常规 HTTP 请求处理
     */
    private void handleNormalHttp(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 这里可以结合自定义注解路由 (@Controller @RequestMapping) 进行分发
        // 此处以简单文本响应为例
        String msg = "Normal HTTP Request handled. URI: " + request.uri();
        sendResponse(ctx, HttpResponseStatus.OK, msg);
    }

    /**
     * 响应构建器
     */
    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                io.netty.buffer.Unpooled.copiedBuffer(content.getBytes())
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        // 非Keep-Alive则写完即关
//        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE); // 写完数据则关闭连接
        ctx.writeAndFlush(response);
    }

    /**
     * 基础类型映射工具
     */
    private Class<?> mapToClass(String typeName) {
        switch (typeName) {
            case "int": return int.class;
            case "long": return long.class;
            case "boolean": return boolean.class;
            case "double": return double.class;
            case "float": return float.class;
            default:
                try { return Class.forName(typeName); }
                catch (ClassNotFoundException e) { return Object.class; }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
        ctx.close();
    }
}
