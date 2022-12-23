import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Demo02 {
    public static void main(String[] args) {
//        ByteBuffer buffer = StandardCharsets.UTF_8.encode("你好");
//        ByteBufferUtil.debugAll(buffer);
//        System.out.println(buffer.getClass());
        ByteBuffer source = ByteBuffer.allocate(32);
        source.put("Hello,world\nI'm zhangsan\nHo".getBytes(StandardCharsets.UTF_8));
        split(source);
        ByteBufferUtil.debugAll(source);
        source.put("w are you?\nhaha!\n".getBytes());
        split(source);


    }
    public static void split(ByteBuffer source) {
        source.flip();
        int oldLimit = source.limit();
        for (int i = 0; i < oldLimit; i++) {
            // get(i)拿到i但是指针不动，get()会移动一次
            if (source.get(i) == '\n') {
                System.out.println(i);
                // 两个索引之间的距离计算要加一
                ByteBuffer target = ByteBuffer.allocate(i + 1 - source.position());
                // 更改limit的位置，limit是position不能到达的位置
                source.limit(i + 1);
                // put完成后position肯定和limit位置一样，下一次就还是从这里读
                System.out.println(source.limit() + "ds" + source.position());
                // 这个put方法直接放source的position到limit的数据，target长度不够就会异常
                target.put(source);
                ByteBufferUtil.debugAll(target);
                // get（i）的时候会检查i的位置是否大于limit，所以每次都要回置
                source.limit(oldLimit);
            }
        }
        // compact即保住了未读的buffer，而且下次用这个buffer放数据会从未读的buffer位置后面开始填充，buffer这个结构太牛了！
        source.compact();
    }
}
