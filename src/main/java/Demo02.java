import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Demo02 {
    public static void main(String[] args) {
//        ByteBuffer buffer = StandardCharsets.UTF_8.encode("你好");
//        ByteBufferUtil.debugAll(buffer);
//        System.out.println(buffer.getClass());
        ByteBuffer source = ByteBuffer.allocate(32);
        source.put("Hello,world\nI'm zhangsan\nHo".getBytes(StandardCharsets.UTF_8));
        ByteBufferUtil.debugAll(source);
        split(source);
        source.put("w are you?\nhaha!\n".getBytes());
        split(source);

    }
    private static void split(ByteBuffer source) {
        source.flip();
        int oldLimit = source.limit();
        for (int i = 0; i < oldLimit; i++) {
            if (source.get(i) == '\n') {
                System.out.println(i);
                ByteBuffer target = ByteBuffer.allocate(i + 1 - source.position());
                // 更改limit的位置
                source.limit(i + 1);
                // put完成后position肯定和limit位置一样，下一次就还是从这里读
                target.put(source);
                ByteBufferUtil.debugAll(target);
                // get（i）的时候会检查i的位置是否大于limit，所以每次都要回置
                source.limit(oldLimit);
            }
        }

        source.compact();
    }
}
