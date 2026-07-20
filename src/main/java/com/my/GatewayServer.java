package com.my;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author duoyian
 * @date 2026/7/20
 */
public class GatewayServer {
    private final int port;

    public GatewayServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            // 1. HTTP 编解码器 (包含 HttpRequestDecoder 和 HttpResponseEncoder)
                            p.addLast("codec", new HttpServerCodec());
                            // 2. HTTP 消息聚合器，解决 HTTP 拆包问题，最大消息长度 10MB
                            p.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
                            // 3. 心跳检测
                            p.addLast("idleState", new IdleStateHandler(30, 0, 0));
                            // 4. 自定义业务处理器
                            p.addLast("gatewayHandler", new GatewayRequestHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("Gateway Server started on port " + port);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
