package com.kang.nettyim;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Date;

/**
 * Netty 客户端实现
 * 功能：连接到本地 8000 端口，每 2 秒发送一次包含时间戳的消息
 */
public class NettyClient {
    public static void main(String[] args) throws InterruptedException {
        // ==================== 1. 创建客户端启动器 ====================
        /**
         * Bootstrap 是客户端的引导类，负责配置和启动客户端
         * 类比：就像装修时需要先准备好工具箱
         *
         * 为什么需要 Bootstrap？
         * - 统一配置入口：将连接参数、线程模型、处理器等集中管理
         * - 链式调用：通过 builder 模式让配置更清晰
         * - 复用机制：可以用同一个 Bootstrap 创建多个连接
         */
        Bootstrap bootstrap = new Bootstrap();

        // ==================== 2. 创建事件循环组 ====================
        /**
         * NioEventLoopGroup 是 Netty 的事件循环线程池
         * 默认创建 2*CPU核数 个线程（这里没有指定，使用默认）
         *
         * 为什么需要事件循环组？
         * - 非阻塞 IO：通过多路复用技术处理多个连接
         * - 事件驱动：当有 IO 事件（读、写、连接）时自动处理
         * - 线程隔离：IO 操作由专门线程处理，不会阻塞主线程
         *
         * 注意：事件循环组使用完需要调用 shutdownGracefully() 释放资源
         */
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup();

        // ==================== 3. 配置启动器 ====================
        bootstrap
                // --- 3.1 设置事件循环组 ---
                /**
                 * group(eventExecutors)
                 * 将之前创建的事件循环组分配给 Bootstrap
                 *
                 * 为什么要把事件循环组设置给 Bootstrap？
                 * - Bootstrap 本身不创建线程，需要从 eventExecutors 借用
                 * - 所有 IO 操作都在这个线程池中执行
                 */
                .group(eventExecutors)

                // --- 3.2 指定 Channel 类型 ---
                /**
                 * channel(NioSocketChannel.class)
                 * 指定底层使用的 Socket Channel 实现类
                 *
                 * 为什么是 NioSocketChannel？
                 * - Nio 代表使用 Java NIO（New IO）库实现非阻塞 IO
                 * - Socket 代表这是客户端 Socket（服务端用 NioServerSocketChannel）
                 * - 反射创建：Netty 会在内部通过反射创建该类的实例
                 *
                 * 其他可选实现：
                 * - EpollSocketChannel (Linux 高性能实现)
                 * - KQueueSocketChannel (MacOS 实现)
                 * - OioSocketChannel (旧的阻塞式 IO，不推荐)
                 */
                .channel(NioSocketChannel.class)

                // --- 3.3 设置处理器 ---
                /**
                 * handler(new ChannelInitializer<NioSocketChannel>())
                 * 设置连接建立后的初始化处理器
                 *
                 * ChannelInitializer 是什么？
                 * - 是一个特殊的 ChannelHandler，用于初始化新连接
                 * - 每个新连接创建时都会调用一次 initChannel 方法
                 * - 执行完初始化后会自动从 ChannelPipeline 中移除自己
                 *
                 * 为什么要用泛型 <NioSocketChannel>？
                 * - 类型安全：确保 initChannel 参数类型匹配
                 * - 明确告诉 Netty 这个初始化器是为 NioSocketChannel 准备的
                 * - 避免在 initChannel 中做类型强转
                 */
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    /**
                     * initChannel 方法
                     * 在新连接建立时被调用，用于配置这个连接的处理器链
                     *
                     * 为什么设计成回调方法？
                     * - 时机控制：Netty 确保在连接完全准备好后才初始化
                     * - 每个连接独立：每个连接都有自己的 ChannelPipeline
                     * - 资源管理：适合在这里添加编码器、解码器、业务处理器
                     *
                     * @param channel 新建立的客户端 Socket Channel
                     */
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        /**
                         * channel.pipeline() 获取管道对象
                         * ChannelPipeline 是什么？
                         * - 处理器链容器：负责管理多个 ChannelHandler
                         * - 顺序执行：数据会按添加顺序经过每个处理器
                         * - 双向流动：
                         *   * 出站（写入）按 addLast 的倒序处理
                         *   * 入站（读取）按 addLast 的顺序处理
                         *
                         * 为什么需要管道？
                         * - 关注点分离：不同的处理器负责不同的功能
                         * - 灵活组合：可以动态添加/删除处理器
                         * - 代码复用：同一个处理器可以在多个连接中复用
                         */
                        channel.pipeline()
                                /**
                                 * addLast(new StringEncoder())
                                 * 添加字符串编码器到管道的最后一个位置
                                 *
                                 * StringEncoder 的作用：
                                 * - 将 String 对象编码为 ByteBuf（Netty 的字节容器）
                                 * - 出站处理器：只在发送数据时生效
                                 * - 编码过程：String → ByteBuf → 网络传输
                                 *
                                 * 为什么需要编码器？
                                 * - 网络传输只能传字节流
                                 * - Java 对象需要序列化成字节才能发送
                                 * - Netty 提供了多种编码器处理不同数据格式
                                 *
                                 * addLast 的位置影响：
                                 * - 对于出站数据，addLast 添加的处理器后添加的先执行
                                 * - 先执行编码器，将字符串转为字节
                                 * - 再执行后续的处理器（如果有的话）
                                 */
                                .addLast(new StringEncoder());
                    }
                });

        // ==================== 4. 建立连接 ====================
        /**
         * bootstrap.connect("127.0.0.1", 8000)
         * 连接到服务器
         *
         * 为什么返回 ChannelFuture？
         * - 异步连接：Netty 的操作都是异步的
         * - 非阻塞：connect() 方法立即返回，实际连接在后台进行
         * - 通知机制：可以添加监听器来监听连接结果
         *
         * .channel()
         * 获取实际连接的 Channel 对象
         *
         * 为什么通过 ChannelFuture 获取？
         * - channel() 方法会等待连接完成
         * - 如果连接失败，这里会抛出异常
         * - 确保后续操作时连接已就绪
         *
         * 参数说明：
         * - "127.0.0.1": 本地回环地址，只能本机访问
         * - 8000: 目标端口号
         */
        Channel channel = bootstrap.connect("127.0.0.1", 8000)
                .channel();  // 同步等待连接建立，获取 Channel

        // ==================== 5. 发送消息循环 ====================
        /**
         * 无限循环发送消息
         *
         * 为什么用 while(true) 而不是定时器？
         * - 简单直接：适合演示和简单场景
         * - 主线程阻塞：保持客户端不退出
         * - 不推荐生产使用：应该使用 EventLoop 的定时任务
         *
         * 更好的做法：
         * channel.eventLoop().scheduleAtFixedRate(() -> {
         *     channel.writeAndFlush(new Date() + ": hello world");
         * }, 0, 2, TimeUnit.SECONDS);
         */
        while (true) {
            /**
             * channel.writeAndFlush(new Date() + ": hello world")
             * 发送消息并立即刷新缓冲区
             *
             * 为什么用 writeAndFlush 而不是 write？
             * - write()：只将数据写入 Netty 的发送缓冲区
             * - writeAndFlush()：写入后立即发送，确保数据及时到达
             *
             * 对比：channel.write(msg).addListener(future -> channel.flush());
             * - writeAndFlush 是 write + flush 的简写
             * - flush() 强制将缓冲区数据发送到网络
             *
             * 为什么需要 flush？
             * - 批量发送优化：Netty 会缓存多次 write，一次性 flush
             * - 降低系统调用次数：提高网络效率
             *
             * 发送流程：
             * 1. String 对象进入 StringEncoder
             * 2. 编码为 ByteBuf 字节数据
             * 3. 写入 Channel 的发送缓冲区
             * 4. 发送到网络
             * 5. 服务器接收并处理
             */
            channel.writeAndFlush(new Date() + ": hello world");

            /**
             * Thread.sleep(2000L)
             * 休眠 2 秒
             *
             * 为什么需要休眠？
             * - 控制发送频率：避免消息洪泛
             * - 模拟实际业务场景：客户端通常有固定的上报周期
             * - 可观察性：在控制台能看到间隔输出的日志
             *
             * 为什么不使用 ScheduledExecutorService？
             * - 代码更简单：适合学习和演示
             * - 缺点：阻塞主线程，不能做其他事情
             *
             * 生产环境建议：
             * channel.eventLoop().scheduleAtFixedRate(...) 
             * 由 Netty 的事件循环管理定时任务
             */
            Thread.sleep(2000L);
        }

        // 注意：由于 while(true) 循环，代码永远不会执行到这里
        // 实际项目中应该优雅关闭：
        // eventExecutors.shutdownGracefully();
        // channel.closeFuture().sync();
    }
}