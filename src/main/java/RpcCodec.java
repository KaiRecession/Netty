import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;

@Slf4j
public class RpcCodec extends ByteToMessageCodec<Object>{

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        // 使用jdk的序列化方式
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        System.out.println(o);
        oos.writeObject(o);
        byte[] bytes = bos.toByteArray();
        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);
    }
   // 长度字段已被爷提前剥离
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        byte[] bytes = new byte[byteBuf.readableBytes()];

        byteBuf.readBytes(bytes);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object o = ois.readObject();
        // add一定要加上，不然就传的空值！
        list.add(o);
    }
}
