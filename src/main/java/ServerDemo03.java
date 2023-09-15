import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class ServerDemo03 {
    public static void main(String[] args) {
        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            channel.bind(new InetSocketAddress(8080));
            System.out.println(channel);
            Selector selector = Selector.open();
            channel.configureBlocking(false);
            // selector只关心注册的事件类型
            channel.register(selector, SelectionKey.OP_ACCEPT);
            HashMap<Integer, Integer> ii = new HashMap<>();
            ii.put(1, 2);
            LinkedList<Object> objects = new LinkedList<>();

            System.out.println(ii.get(3));
            while (true) {
                int count = selector.select();
                log.debug("select count:{}", count);
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        ServerSocketChannel c = (ServerSocketChannel)key.channel();
                        SocketChannel sc = c.accept();
                        sc.configureBlocking(false);
                        ByteBuffer buffer = ByteBuffer.allocate(16);
                        // 和threadLocal类似
                        sc.register(selector, SelectionKey.OP_READ, buffer);
                        log.debug("连接已经建立: {}", sc);


                    } else if (key.isReadable()) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        ByteBuffer attachment = (ByteBuffer) key.attachment();
                        int read = sc.read(attachment);
                        System.out.println("读取的字节数：" + read);
                        if (read == -1) {
                            key.cancel();
                            sc.close();
                        } else {
                            Demo02.split(attachment);
                            if (attachment.position() == attachment.limit()) {
                                ByteBuffer newBuffer = ByteBuffer.allocate(attachment.capacity() * 2);
                                // flip方法看源码就是把limit等于position，再让position等于0
                                attachment.flip();
                                newBuffer.put(attachment);
                                key.attach(newBuffer);
                            }
//                            ByteBufferUtil.debugAll(buffer);
                        }
                    }
                    // 读的时候才会有异常
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
