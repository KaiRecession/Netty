import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientDemo02 {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080)) {
            System.out.println(socket);
            socket.getOutputStream().write("word".getBytes(StandardCharsets.UTF_8));
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
