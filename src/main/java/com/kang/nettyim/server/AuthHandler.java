package com.kang.nettyim.server;

import com.kang.nettyim.protocol.LoginUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!LoginUtil.isLogin(ctx.channel())) {
            ctx.channel().close();
            return;
        }
        ctx.pipeline().remove(this);
        super.channelRead(ctx, msg);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (LoginUtil.isLogin(ctx.channel())) {
            System.out.println("当前连接登录验证完毕，无须再次验证, AuthHandler 被移除");
        } else {
            System.out.println("未登录验证，强制关闭连接！");
        }
    }
}
