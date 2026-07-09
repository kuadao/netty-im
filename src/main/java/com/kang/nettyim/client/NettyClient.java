package com.kang.nettyim.client;

import com.kang.nettyim.protocol.LoginUtil;
import com.kang.nettyim.protocol.PacketCodeC;
import com.kang.nettyim.protocol.request.MessageRequestPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Date;
import java.util.Scanner;

public class NettyClient {


    public static void main(String[] args) throws InterruptedException {

        NioEventLoopGroup eventExecutors = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                             @Override
                             protected void initChannel(SocketChannel ch) {
                                 ch.pipeline().addLast(new ClientHandler());
                             }
                         }
                )
                .connect("127.0.0.1", 6000)
                .addListener(future -> {
                    if (future.isSuccess()) {
                        System.out.println(new Date() + ": 连接服务器成功");
                    } else {
                        System.out.println(new Date() + ": 未成功连接到服务器");
                    }
                }).sync();

    }

    public static void startConsoleThread(Channel channel) {
        new Thread(() -> {
            System.out.println("输入消息发送至服务端:");

            Scanner scanner = new Scanner(System.in); // 只创建一次
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if ("quit".equalsIgnoreCase(line)) {
                    channel.close();
                    break;
                }

                MessageRequestPacket packet = new MessageRequestPacket();
                packet.setMessage(line);
                ByteBuf out = PacketCodeC.INSTANCE.encode(packet, channel.alloc());

                // Netty 保证跨线程调用 writeAndFlush 是绝对安全的
                channel.writeAndFlush(out);
            }
        }).start();
    }
}
