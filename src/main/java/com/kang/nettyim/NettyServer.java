package com.kang.nettyim;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

/**
 * Netty 服务器实现
 * 功能：监听 8000 端口，接收客户端发送的字符串消息并打印到控制台
 */
public class NettyServer {
    public static void main(String[] args) {
        // ==================== 1. 创建服务器启动器 ====================
        /**
         * ServerBootstrap 是服务端的引导类
         *
         * 与客户端的 Bootstrap 区别：
         * - Bootstrap：客户端专用，只配置一个 EventLoopGroup
         * - ServerBootstrap：服务端专用，可以配置两个 EventLoopGroup（主从模式）
         *
         * ServerBootstrap 特有功能：
         * - 支持主从 Reactor 线程模型
         * - 可以设置子 Channel 的配置（childHandler, childOption 等）
         * - 绑定端口后会返回 ChannelFuture
         *
         * 为什么服务端需要专门的 Bootstrap？
         * - 服务端需要处理两个层面的连接：
         *   1. 监听端口接受新连接（ServerSocketChannel）
         *   2. 处理已建立连接的数据读写（SocketChannel）
         * - 客户端只需要处理一个连接，相对简单
         */
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        // ==================== 2. 创建事件循环组（主从 Reactor 模型）====================
        /**
         * Boss Group（老板线程组）
         * - 默认 1 个线程（Netty 会设置为 1）
         * - 只负责接受新连接（accept）
         * - 不处理任何 IO 读写操作
         *
         * 为什么只需要 1 个线程？
         * - 服务端通常只有一个监听端口
         * - accept 操作不耗时，一个线程完全够用
         * - 即使有多个端口，也可以共用一个 Boss 线程
         *
         * Boss 线程的工作流程：
         * 1. 监听 ServerSocketChannel 的 OP_ACCEPT 事件
         * 2. 当新连接到来时，调用 accept() 方法
         * 3. 将新创建的 SocketChannel 注册到 Worker Group 的某个 EventLoop
         * 4. 继续监听新连接
         */
        NioEventLoopGroup boss = new NioEventLoopGroup(1, r -> {
            Thread thread = new Thread();
            thread.setDaemon(true);
            return thread;
        });

        /**
         * Worker Group（工人线程组）
         * - 默认 2*CPU核心数 个线程
         * - 负责处理已建立连接的 IO 操作（读写）和业务处理
         * - 每个 Worker 线程管理多个 SocketChannel
         *
         * 为什么需要多个 Worker 线程？
         * - 充分利用多核 CPU 并行处理
         * - 避免单线程处理大量连接成为瓶颈
         * - 每个线程处理一部分连接，负载均衡
         *
         * Worker 线程的工作流程：
         * 1. 管理分配给自己的所有 SocketChannel
         * 2. 监听这些 Channel 的 OP_READ/OP_WRITE 事件
         * 3. 当事件发生时，调用对应的 ChannelHandler 处理
         * 4. 处理 Pipeline 中的所有 Handler
         */
        NioEventLoopGroup worker = new NioEventLoopGroup(4, r -> {
            Thread thread = new Thread();
            thread.setDaemon(true);
            return thread;
        });

        // ==================== 3. 配置服务器启动器 ====================
        serverBootstrap
                /**
                 * 设置主从线程组
                 * group(boss, worker)
                 * - boss 参数：处理连接事件的线程组
                 * - worker 参数：处理 IO 事件的线程组
                 *
                 * 为什么需要两个线程组？
                 * 1. 职责分离：
                 *    - Boss 只做连接管理
                 *    - Worker 只做 IO 处理
                 * 2. 避免阻塞：
                 *    - 如果 accept 和处理混在一起
                 *    - 高并发时新连接可能等待太久
                 * 3. 资源隔离：
                 *    - Boss 线程很空闲，不会被 IO 处理拖累
                 *    - Worker 线程很忙碌，不会影响新连接接受
                 *
                 * 主从 Reactor 模型示意：
                 * Boss 线程                    Worker 线程池
                 *   |                              |
                 *   ├─ 接收连接1 ──→ Worker1 处理  ├─ 连接1、2、3的IO
                 *   ├─ 接收连接2 ──→ Worker2 处理  ├─ 连接4、5、6的IO
                 *   ├─ 接收连接3 ──→ Worker1 处理  ├─ 连接7、8、9的IO
                 *   └─ 接收连接4 ──→ Worker3 处理  └─ ...
                 */
                .group(boss, worker)

                /**
                 * 设置服务端 Channel 类型
                 * channel(NioServerSocketChannel.class)
                 *
                 * NioServerSocketChannel vs NioSocketChannel：
                 * - NioServerSocketChannel：服务端专用，代表监听端口
                 *   * 有 ServerSocketChannel 底层实现
                 *   * 只处理 ACCEPT 事件
                 *   * 由 Boss 线程管理
                 *
                 * - NioSocketChannel：代表已建立的连接
                 *   * 有 SocketChannel 底层实现
                 *   * 处理 READ/WRITE 事件
                 *   * 由 Worker 线程管理
                 *
                 * 对应关系：
                 * ServerBootstrap → NioServerSocketChannel（监听端口）
                 * 每个客户端连接 → NioSocketChannel（数据传输）
                 */
                .channel(NioServerSocketChannel.class)

                /**
                 * 设置子 Channel 的初始化处理器
                 * childHandler(new ChannelInitializer<NioSocketChannel>())
                 *
                 * handler() vs childHandler() 的区别：
                 * - handler()：设置服务端 Channel（NioServerSocketChannel）的处理器
                 *   * 用于处理 accept 事件
                 *   * 例如：添加日志处理器、连接计数器等
                 *
                 * - childHandler()：设置子 Channel（NioSocketChannel）的处理器
                 *   * 用于处理每个客户端连接的 IO 事件
                 *   * 每个新连接都会创建新的 ChannelInitializer 实例
                 *   * 这是业务逻辑处理的核心入口
                 *
                 * 为什么叫 childHandler？
                 * - 服务端 Channel 是"父"（parent）
                 * - 客户端连接是"子"（child）
                 * - childHandler 为每个子 Channel 设置处理器
                 *
                 * ChannelInitializer 的执行时机：
                 * 1. 当新连接被 accept 后
                 * 2. Boss 线程将其注册到 Worker 线程时
                 * 3. Worker 线程调用 initChannel 初始化
                 * 4. 初始化完成后，ChannelInitializer 自动从 Pipeline 移除
                 */
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    /**
                     * 初始化每个客户端连接
                     *
                     * @param nioSocketChannel 刚建立的客户端连接对象
                     *
                     * 为什么要在这里初始化？
                     * - 每个连接需要独立的 Handler 实例（或共享，取决于实现）
                     * - 确保 Channel 已经完全初始化，可以安全添加 Handler
                     * - 所有 Handler 在 Channel 注册到 EventLoop 后、正式开始处理数据前添加
                     */
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) {
                        /**
                         * 获取当前连接的 Pipeline（管道）
                         *
                         * ChannelPipeline 的双向链表结构：
                         * 入站方向（读取数据）：从头到尾
                         * Head → StringDecoder → SimpleChannelInboundHandler → Tail
                         *
                         * 出站方向（写入数据）：从尾到头
                         * Tail → SimpleChannelInboundHandler → StringDecoder → Head
                         */
                        nioSocketChannel.pipeline()
                                /**
                                 * 添加字符串解码器
                                 * addLast(new StringDecoder())
                                 *
                                 * StringDecoder 的作用：
                                 * - 将 ByteBuf（字节数据）解码为 String 对象
                                 * - 入站处理器：只在接收数据时生效
                                 * - 解码过程：网络字节流 → ByteBuf → String
                                 *
                                 * 为什么需要解码器？
                                 * - 网络传输的原始数据是字节流
                                 * - 上层应用需要处理 Java 对象（这里需要 String）
                                 * - 解码器负责将字节转换为业务可用的对象
                                 *
                                 * StringDecoder 的内部逻辑：
                                 * 1. 读取 ByteBuf 中的字节
                                 * 2. 使用 UTF-8 字符集（默认）解码为 String
                                 * 3. 将 String 对象传递给下一个 Handler
                                 *
                                 * 注意：StringDecoder 是 Sharable 的，可以多个连接共享
                                 * 这里每次 addLast 虽然创建新实例，但实际可以共用（线程安全）
                                 */
                                .addLast(new StringDecoder())

                                /**
                                 * 添加业务处理器
                                 * addLast(new SimpleChannelInboundHandler<String>())
                                 *
                                 * SimpleChannelInboundHandler 是什么？
                                 * - 简化版的 ChannelInboundHandler
                                 * - 自动释放接收到的消息（防止内存泄漏）
                                 * - 泛型指定要处理的消息类型
                                 *
                                 * 泛型 <String> 的作用：
                                 * - 告诉 Netty 只处理 String 类型的消息
                                 * - 不是 String 类型的消息会自动跳过
                                 * - 避免手动类型转换和类型判断
                                 *
                                 * 为什么不用普通的 ChannelInboundHandler？
                                 * - ChannelInboundHandler：需要手动释放消息
                                 *   public void channelRead(ctx, msg) {
                                 *       String s = (String) msg;
                                 *       // 处理消息
                                 *       ReferenceCountUtil.release(msg); // 必须手动释放
                                 *   }
                                 *
                                 * - SimpleChannelInboundHandler：自动释放
                                 *   public void channelRead0(ctx, msg) {
                                 *       // msg 已经自动转换和释放
                                 *   }
                                 */
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    /**
                                     * 消息处理方法
                                     * channelRead0 是模板方法模式的体现
                                     *
                                     * @param channelHandlerContext 通道处理器上下文
                                     * @param s 解码后的字符串消息
                                     *
                                     * channelHandlerContext 的作用：
                                     * - 获取当前 Channel 对象
                                     * - 获取当前 Pipeline
                                     * - 触发下一个 Handler
                                     * - 获取或设置 Channel 的属性
                                     *
                                     * 为什么设计成 channelRead0 而不是 channelRead？
                                     * - 方法名加 "0" 表示这是内部实现
                                     * - 自动类型转换：只接收 String 类型
                                     * - 自动资源释放：方法执行后自动释放消息
                                     * - 简化代码：开发者不需要关心消息释放
                                     *
                                     * 执行流程：
                                     * 1. Netty 读取到网络数据
                                     * 2. ByteBuf 进入 Pipeline
                                     * 3. StringDecoder 将 ByteBuf 解码为 String
                                     * 4. String 消息进入 SimpleChannelInboundHandler
                                     * 5. 调用 channelRead0 方法
                                     * 6. 执行业务逻辑（这里是打印）
                                     * 7. 自动释放消息对象
                                     */
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                                                String s) {
                                        /**
                                         * 打印接收到的消息
                                         * System.out.println(s)
                                         *
                                         * 这里是最简单的业务处理：打印到控制台
                                         *
                                         * 实际项目中的典型操作：
                                         * - 日志记录
                                         * - 业务逻辑处理
                                         * - 数据库操作
                                         * - 返回响应数据
                                         *
                                         * 例如：
                                         * // 处理消息后回写响应
                                         * String response = processMessage(s);
                                         * channelHandlerContext.writeAndFlush(response);
                                         */
                                        System.out.println(s);

                                        /**
                                         * 如何发送响应（如果需要）：
                                         * channelHandlerContext.writeAndFlush("收到: " + s);
                                         *
                                         * writeAndFlush vs write：
                                         * - write：写入缓冲区，不立即发送
                                         * - writeAndFlush：立即发送
                                         *
                                         * 为什么回写时用 writeAndFlush？
                                         * - 及时响应客户端
                                         * - 如果不用 flush，客户端可能一直等待
                                         */
                                    }
                                });
                    }
                })

                /**
                 * 绑定端口
                 * bind(8000)
                 *
                 * bind 方法的作用：
                 * - 将服务绑定到指定端口
                 * - 开始监听客户端连接
                 * - 返回 ChannelFuture 对象
                 *
                 * 为什么是异步绑定？
                 * - bind() 立即返回，实际绑定在后台进行
                 * - 可以添加监听器等待绑定完成
                 * - 避免阻塞主线程
                 *
                 * 改进写法（生产环境推荐）：
                 * serverBootstrap.bind(8000).sync()
                 *     .channel().closeFuture().sync();
                 *
                 * 当前代码的问题：
                 * - 没有等待绑定完成就主线程结束
                 * - 服务可能还没启动程序就退出了
                 * - 没有阻塞主线程保持服务器运行
                 *
                 * 为什么这段代码可能"刚好能运行"？
                 *
                 * NioEventLoopGroup 创建的线程默认是非守护线程
                 * Java 规范：只要存在非守护线程，JVM 就不会退出
                 * main 线程结束不等于 JVM 退出
                 * 程序看起来"阻塞"是因为 JVM 在等待非守护线程结束
                 */
                .bind(8000);

        /**
         * ==================== 4. 代码存在的问题和改进 ====================
         *
         * 问题1：主线程可能提前退出
         * 原因：bind() 是异步的，main 方法执行完就退出
         * 解决：等待绑定完成并阻塞主线程
         *
         * 问题2：没有优雅关闭
         * 原因：强制关闭可能导致数据丢失
         * 解决：添加 JVM 关闭钩子
         *
         * 问题3：没有异常处理
         * 原因：网络操作可能失败
         * 解决：try-catch 或使用 Future 的异常回调
         */

        // ==================== 改进后的完整版本 ====================
        /**
         * 生产环境推荐写法：
         * <p>
         * ChannelFuture future = serverBootstrap.bind(8000).sync();
         *
         * // 添加关闭钩子
         * Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         *     boss.shutdownGracefully();
         *     worker.shutdownGracefully();
         * }));
         *
         * // 阻塞直到服务器 Channel 关闭
         * future.channel().closeFuture().sync();
         *
         * // 优雅关闭
         * boss.shutdownGracefully();
         * worker.shutdownGracefully();
         */


        System.out.println("bind() 返回了，main 线程继续执行");
        System.out.println("main 线程结束！");
    }
}