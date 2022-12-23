import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;

public class ServerDmoe04 {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        ssc.bind(new InetSocketAddress(8080));
        while (true) {
            selector.select();
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    // 可以通过这个key添加事件类型，没啥别的作用
                    SelectionKey sckey = sc.register(selector, 0, null);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 30000000; i++) {
                        sb.append('a');
                    }
                    ByteBuffer buffer = Charset.defaultCharset().encode(sb.toString());
                    // 把数据发送完
                    // 返回实际写入的字节数
                    int write = sc.write(buffer);
                    System.out.println(write);
                    if (buffer.hasRemaining()) {
                        // 第一次没有发送完，开始处理，首先关注一个可写事件
                        sckey.interestOps(sckey.interestOps() + SelectionKey.OP_WRITE);
                        // 把buffer挂在附件上
                        sckey.attach(buffer);
                    }
                } else if (key.isWritable()) {
                    // 这个可写事件简直就是通知什么时候拥塞控制放开了，我们就去写
                    ByteBuffer attachment = (ByteBuffer) key.attachment();
                    SocketChannel sc = (SocketChannel) key.channel();
                    int write = sc.write(attachment);
                    System.out.println("实际写入字节：" + write);
                    if (!attachment.hasRemaining()) {
                        // 取消可写事件，否则写完了数据后，这个可写事件会一直触发
                        key.interestOps(key.interestOps() - SelectionKey.OP_WRITE);
                        key.attach(null);
                    }
                }
            }
        }

    }
}
