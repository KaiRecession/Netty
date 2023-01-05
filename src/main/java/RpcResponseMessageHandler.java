import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class RpcResponseMessageHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponseMessage msg) throws Exception {
        log.debug("服务器返回的结果{}", msg);
        Exception exceptionVlaue = msg.getExceptionVlaue();
        if (exceptionVlaue != null) {
            RpcClient.promise.setFailure(exceptionVlaue);
        } else {
            RpcClient.promise.setSuccess(msg.getReturnValue());

        }
    }
}
