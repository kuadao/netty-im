package com.kang.nettyim.client;

import com.kang.nettyim.protocol.LoginUtil;
import com.kang.nettyim.protocol.request.LoginRequestPacket;
import com.kang.nettyim.protocol.response.LoginResponsePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Date;

public class LoginResponseHandler extends SimpleChannelInboundHandler<LoginResponsePacket> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(new Date() + ": 客户端登录开始");

        LoginRequestPacket loginRequestPacket = new LoginRequestPacket();
        loginRequestPacket.setUserId(Integer.MAX_VALUE);
        loginRequestPacket.setUsername("张三");
        loginRequestPacket.setPassword("10086");
//        ctx.channel().writeAndFlush(loginRequestPacket);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginResponsePacket msg) throws Exception {
        System.out.println(new Date() + ": 收到服务端消息 - " + msg.getReason());
//        if (msg.isSuccess()) {
//            LoginUtil.markLogin(ctx.channel());
//            System.out.println(new Date() + ": 客户端登录成功");
//            NettyClient.startConsoleThread(ctx.channel());
//        } else {
//            System.out.println(new Date() + ": 客户端登录失败");
//        }
//        NettyClient.startConsoleThread(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("客户端连接被关闭！");
    }
}
