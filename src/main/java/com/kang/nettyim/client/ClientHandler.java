package com.kang.nettyim.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.kang.nettyim.protocol.*;
import com.kang.nettyim.protocol.request.LoginRequestPacket;
import com.kang.nettyim.protocol.response.LoginResponsePacket;
import com.kang.nettyim.protocol.response.MessageResponsePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(new Date() + ": 客户端登录开始");

        LoginRequestPacket loginRequestPacket = new LoginRequestPacket();
        loginRequestPacket.setUserId(Integer.MAX_VALUE);
        loginRequestPacket.setUsername("张三");
        loginRequestPacket.setPassword("10086");
        ByteBuf buffer = PacketCodeC.INSTANCE.encode(loginRequestPacket, ctx.alloc());
        ctx.channel().writeAndFlush(buffer);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        Packet packet = PacketCodeC.INSTANCE.decode(in);
        if (packet instanceof LoginResponsePacket loginResponsePacket) {
            System.out.println(new Date() + ": 收到服务端消息 - " + loginResponsePacket.getReason());
            if (loginResponsePacket.isSuccess()) {
                LoginUtil.markLogin(ctx.channel());
                System.out.println(new Date() + ": 客户端登录成功");
                NettyClient.startConsoleThread(ctx.channel());
            } else {
                System.out.println(new Date() + ": 客户端登录失败");
            }
        } else if (packet instanceof MessageResponsePacket messageResponsePacket) {
            System.out.println(new Date() + ": 收到服务端消息 - " + messageResponsePacket.getMessage());
        }
    }
}
