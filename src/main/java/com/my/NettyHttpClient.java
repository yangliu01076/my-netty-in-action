package com.my;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author duoyian
 * @date 2026/7/21
 */
public class NettyHttpClient {

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8080;

        // 1. 创建客户端的 EventLoopGroup（客户端不需要 BossGroup，只需要一个 WorkerGroup）
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            // 1. HTTP 客户端编解码器
                            p.addLast(new HttpClientCodec());
                            // 2. HTTP 消息聚合器（处理响应体拆包）
                            p.addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                            // 3. 自定义业务处理 Handler（处理服务端返回的响应）
                            p.addLast(new HttpClientResponseHandler());
                        }
                    });

            // 2. 连接到服务端
            ChannelFuture connectFuture = b.connect(host, port).sync();
            Channel channel = connectFuture.channel();

            // 3. 发送常规 GET 请求
            sendHttpGet(channel, "http://127.0.0.1:8080/api/user/query?name=test");

            // 稍微休眠一下，等待第一个请求响应处理完毕
            Thread.sleep(1000);

            // 4. 发送泛化调用 POST 请求
            String jsonBody = "{\"interfaceName\":\"com.my.UserService\",\"methodName\":\"getUserInfo\",\"parameterTypes\":[\"java.lang.String\",\"int\"],\"arguments\":[\"user123\",25]}";
            sendHttpPost(channel, "http://127.0.0.1:8080/api/invoke", jsonBody);

            // 5. 等待连接关闭（如果服务端没主动断开，这里会一直阻塞，或者设置一个固定休眠时间也行）
            Thread.sleep(300000);
            channel.close().sync();

        } finally {
            // 6. 优雅关闭客户端线程组
            group.shutdownGracefully();
        }
    }

    /**
     * 构造并发送 HTTP GET 请求
     */
    private static void sendHttpGet(Channel channel, String url) throws URISyntaxException {
        URI uri = new URI(url);
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath() + "?" + uri.getRawQuery());

        request.headers().set(HttpHeaderNames.HOST, uri.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        System.out.println("\n========== 客户端发送 GET 请求 ==========");
        System.out.println("URL: " + url);

        channel.writeAndFlush(request);
    }

    /**
     * 构造并发送 HTTP POST 请求
     */
    private static void sendHttpPost(Channel channel, String url, String body) throws URISyntaxException {
        URI uri = new URI(url);

        // 将 JSON body 转换为 ByteBuf
        byte[] bodyBytes = body.getBytes(CharsetUtil.UTF_8);
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, uri.getRawPath(),
                io.netty.buffer.Unpooled.wrappedBuffer(bodyBytes));

        // 设置必要的 HTTP Header
        request.headers().set(HttpHeaderNames.HOST, uri.getHost());
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, bodyBytes.length);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        System.out.println("\n========== 客户端发送 POST 请求 ==========");
        System.out.println("URL: " + url);
        System.out.println("Body: " + body);

        channel.writeAndFlush(request);
    }
}
