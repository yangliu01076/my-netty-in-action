package com.my;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;

/**
 * @author duoyian
 * @date 2026/7/21
 */
public class HttpClientResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
        System.out.println("---------- 收到服务端响应 ----------");
        System.out.println("HTTP Status: " + response.status());

        // 打印响应体内容
        String content = response.content().toString(CharsetUtil.UTF_8);
        System.out.println("Response Body: " + content);
        System.out.println("------------------------------------\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
