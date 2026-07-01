package com.kang.nettyim;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyServer {

    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .handler(new ChannelInitializer<NioServerSocketChannel>() {
                    @Override
                    protected void initChannel(NioServerSocketChannel ch) {
                        System.out.println("服务启动中...");
                    }
                })
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {

                    }
                });

        bindPort(bootstrap, 8000);
    }

    private static void bindPort(ServerBootstrap bootstrap, int port) throws InterruptedException {

        bootstrap.bind(port)
                .addListener(
                        future -> {
                            if (future.isSuccess()) {
                                System.out.println("绑定端口成功：" + port);
                            } else {
                                System.out.println("绑定端口失败：" + port);
                                bindPort(bootstrap, port + 1);
                            }
                        }
                ).sync();
    }
}
