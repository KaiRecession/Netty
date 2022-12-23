import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ClientDemo01 {
    public static void main(String[] args) throws IOException, InterruptedException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost", 8080));
        System.out.println("waiting");
        sc.write(StandardCharsets.UTF_8.encode("Hello"));
        Thread.sleep(5000);
        sc.write(StandardCharsets.UTF_8.encode("Hello"));
        Thread.sleep(100000);
    }
}
