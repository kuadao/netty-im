package com.kang.nettyim.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class FirstServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        System.out.println(new Date() + "：服务端读到数据 -> " + in.toString(StandardCharsets.UTF_8));
        System.out.println(new Date() + "：服务端写出数据");
        ByteBuf out = ctx.alloc().buffer();
        out.writeBytes("你好，show me the money".getBytes(StandardCharsets.UTF_8));
        ctx.channel().writeAndFlush(out);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(System.currentTimeMillis() + " FirstServerHandler.channelActive");
        ByteBuf out = ctx.alloc().buffer();
        out.writeBytes("来自服务端的消息 -> 已连接".getBytes(StandardCharsets.UTF_8));
        ctx.channel().writeAndFlush(out);
    }


}
