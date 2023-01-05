import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

@Slf4j
public class RpcClient {
    private volatile static Channel channel = null;
    private static final Object Lock = new Object();
    public static DefaultPromise<Object> promise = null;

    public static void main(String[] args) {
        HelloService service = getProxyService(HelloService.class);
        System.out.println(service.sayHello("zhansan"));
    }
    public static Channel getChannel() {
        if (channel != null) {
            return channel;
        }
        synchronized (Lock) {
            if (channel != null) {
                return channel;
            }
            initChannel();
            return channel;
        }
    }

    private static void initChannel() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        // rpc 响应消息处理器，待实现
        RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(new RpcCodec());
                    ch.pipeline().addLast(RPC_HANDLER);
                }
            });
            channel = bootstrap.connect("localhost", 8080).sync().channel();
//            channel.writeAndFlush("1").addListener(promise -> {
//                if (!promise.isSuccess()) {
//                    Throwable cause = promise.cause();
//                    log.error("error", cause);
//                }
//            });
            channel.closeFuture().addListener(future -> {
                group.shutdownGracefully();
            });
        } catch (Exception e) {
            log.error("client error", e);
        }
    }

    public static <T> T getProxyService(Class<T> serviceClass) {
        ClassLoader loader = serviceClass.getClassLoader();
        Class[] interfaces = {serviceClass};
        // 这里返回的是代理对象，就是和被代理类型一样的对象
        Object o = Proxy.newProxyInstance(loader, interfaces, (Proxy, method, args) -> {
            RpcRequestMessage rpcRequestMessage = new RpcRequestMessage(
                    serviceClass.getName(),
                    method.getName(),
                    method.getReturnType(),
                    method.getParameterTypes(),
                    args
            );
            getChannel().writeAndFlush(rpcRequestMessage);
            // 只是绑定一个线程池，没啥
            promise = new DefaultPromise<>(getChannel().eventLoop());
            promise.await();
            if (promise.isSuccess()) {
                return promise.getNow();
            } else {
                throw new RuntimeException(promise.cause());
            }
        });
        return (T) o;
    }
}
