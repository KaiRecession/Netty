import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientDemo021 {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080)) {
            System.out.println(socket);
            socket.getOutputStream().write("123456\n78435435436363469\n4324234\n".getBytes(StandardCharsets.UTF_8));
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
