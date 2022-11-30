import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

@Slf4j
public class Demo01 {
    public static void main(String[] args) {
        try (RandomAccessFile file = new RandomAccessFile("data", "rw")) {
            FileChannel channel = file.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(10);
            while (true) {
                int len = channel.read(buffer);
                log.debug("读到的字节数：{}", len);
                if (len == -1) {
                    break;
                }
                // 切换读模式
                buffer.flip();
                while (buffer.hasRemaining()) {
                    log.debug("{}", (char)buffer.get());
                }
                buffer.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
