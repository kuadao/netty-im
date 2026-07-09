package com.kang.nettyim.protocol;

import io.netty.channel.Channel;

public class LoginUtil {

    public static void markLogin(Channel channel) {
        channel.attr(Attributes.LOGIN).set(Boolean.TRUE);
    }

    public static boolean isLogin(Channel channel) {
        return channel.hasAttr(Attributes.LOGIN) ? channel.attr(Attributes.LOGIN).get() : false;
    }
}
