package com.kang.nettyim.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.kang.nettyim.protocol.request.LoginRequestPacket;
import com.kang.nettyim.protocol.request.MessageRequestPacket;
import com.kang.nettyim.protocol.response.LoginResponsePacket;
import com.kang.nettyim.protocol.Packet;
import com.kang.nettyim.protocol.PacketCodeC;
import com.kang.nettyim.protocol.response.MessageResponsePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        Packet packet = PacketCodeC.INSTANCE.decode(in);

        if (packet instanceof LoginRequestPacket loginRequestPacket) {
            System.out.println(new Date() + ": 接收到客户端登录请求");
            System.out.println(JSON.toJSONString(loginRequestPacket, JSONWriter.Feature.PrettyFormat));


            LoginResponsePacket loginResponsePacket = new LoginResponsePacket();
            loginResponsePacket.setSuccess(true);
            loginResponsePacket.setReason("校验登录成功");
            loginResponsePacket.setVersion(loginRequestPacket.getVersion());
            ByteBuf out = PacketCodeC.INSTANCE.encode(loginResponsePacket, ctx.alloc());
            ctx.writeAndFlush(out);
        } else if (packet instanceof MessageRequestPacket messageRequestPacket) {
            System.out.println(new Date() + ": 收到客户端消息");
            System.out.println(JSON.toJSONString(messageRequestPacket, JSONWriter.Feature.PrettyFormat));

            MessageResponsePacket messageResponsePacket = new MessageResponsePacket();
            messageResponsePacket.setMessage("服务端回复【" + messageRequestPacket.getMessage() + "】");
            ByteBuf out = PacketCodeC.INSTANCE.encode(messageResponsePacket, ctx.alloc());
            ctx.writeAndFlush(out);
        }
    }
}
