import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class NettyClientDemo02 {
    public static void main(String[] args) throws InterruptedException {
        Channel channel = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect("127.0.0.1", 8080)
                .sync()
                .channel();
        Scanner sc = new Scanner(System.in);
        while (true) {
            ByteBuf buffer = channel.alloc().buffer();
            // 填充header1
            for (int i = 0; i < 2; i++) {
                buffer.writeByte(1);
            }
            System.out.println("内容：");
            String s = sc.nextLine();
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            // 填充length内容
            buffer.writeByte((byte)bytes.length);
            // 填充header2
            for (int i = 0; i < 2; i++) {
                buffer.writeByte(2);
            }
            // 写入内容
            for (byte aByte : bytes) {
                buffer.writeByte(aByte);
            }
            ChannelFuture channelFuture = channel.writeAndFlush(buffer);
            channelFuture.sync();
        }
    }
}
