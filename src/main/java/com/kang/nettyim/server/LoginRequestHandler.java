package com.kang.nettyim.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.kang.nettyim.protocol.request.LoginRequestPacket;
import com.kang.nettyim.protocol.response.LoginResponsePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Date;

public class LoginRequestHandler extends SimpleChannelInboundHandler<LoginRequestPacket> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestPacket msg) throws Exception {
        System.out.println(new Date() + ": 接收到客户端登录请求");
        System.out.println(JSON.toJSONString(msg, JSONWriter.Feature.PrettyFormat));


        LoginResponsePacket loginResponsePacket = new LoginResponsePacket();
        loginResponsePacket.setSuccess(true);
        loginResponsePacket.setReason("校验登录成功");
        loginResponsePacket.setVersion(msg.getVersion());
        ctx.channel().writeAndFlush(loginResponsePacket);
    }
}
