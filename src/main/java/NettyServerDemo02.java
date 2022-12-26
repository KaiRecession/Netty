import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServerDemo02 {
    public static void main(String[] args) throws InterruptedException {
        DefaultEventLoopGroup normalWorkers = new DefaultEventLoopGroup(2);
        new ServerBootstrap()
                .group(new NioEventLoopGroup(1), new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch)  {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        // 从这个步骤开始，就从另一个线程池拿线程了，处理IO事件的线程就可以空闲了
                        ch.pipeline().addLast(normalWorkers,"myhandler",
                                new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ByteBuf byteBuf = msg instanceof ByteBuf ? ((ByteBuf) msg) : null;
                                        if (byteBuf != null) {
                                            byte[] buf = new byte[16];
                                            ByteBuf len = byteBuf.readBytes(buf, 0, byteBuf.readableBytes());
                                            log.debug(new String(buf));
                                            // 调用这个read方法，还是当前这个线程池的线程，所以后面就一直是这个线程池
                                            ctx.fireChannelRead(msg);
                                        }
                                    }
                                });
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                log.debug("2");
                                ctx.fireChannelRead(msg); // 2
                            }
                        });

                    }
                }).bind(8080).sync();

    }
}
