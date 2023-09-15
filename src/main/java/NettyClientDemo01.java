import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class NettyClientDemo01 {
    public static void main(String[] args) throws InterruptedException {
        new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel channel) throws Exception {
//                        channel.pipeline().addLast(new StringEncoder());
                        channel.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        channel.pipeline().addLast(new ChannelOutboundHandlerAdapter(){
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg,
                                              ChannelPromise promise) {
                                System.out.println(msg);
                                ByteBufAllocator bufAllocator = channel.alloc();
                                ByteBuf buf = bufAllocator.buffer();
                                buf.writeBytes(msg.toString().getBytes(StandardCharsets.UTF_8));
//                                System.out.println(buf.readerIndex());
//                                System.out.println(buf.array().length);
                                //  是从当前节点找上一个出站处理器，如果这句话放在入站处理器前，会找入站处理器的上一个吧
                                ctx.write(buf, promise); // 4
                            }
                        });
                    }
                })
                .connect("127.0.0.1", 8080)
                .sync()
                .channel()
                ;
    }
}
