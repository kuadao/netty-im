package com.kang.nettyim.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class FirstClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        System.out.println(new Date() + "：客户端读到数据 -> " + byteBuf.toString(StandardCharsets.UTF_8));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(System.currentTimeMillis() + "FirstClientHandler.channelActive");

        System.out.println(new Date() + "：客户端写出数据");

        ByteBuf buffer = ctx.alloc().buffer();

        byte[] bytes = "你好，张三".getBytes(StandardCharsets.UTF_8);

        buffer.writeBytes(bytes);

        ctx.channel().writeAndFlush(buffer);
    }
}
