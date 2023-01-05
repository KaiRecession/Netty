import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class RpcResponseMessage implements Serializable {
    private Object returnValue;
    private Exception exceptionVlaue;

    public RpcResponseMessage(Object returnValue, Exception exceptionVlaue) {
        this.returnValue = returnValue;
        this.exceptionVlaue = exceptionVlaue;
    }

    public RpcResponseMessage() {
    }
}
