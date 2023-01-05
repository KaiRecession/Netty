import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

// ? 和 T都表示不确定类型，？用在只用一次，T用来确定多次使用的是同一个
@Getter
@ToString
public class RpcRequestMessage implements Serializable {
    private String interfaceName;
    private String methodName;
    private Class<?> returnType;
    private Class<?>[] paramaterTypes;
    private Object[] parameterValue;

    public RpcRequestMessage(String interfaceName, String methodName, Class<?> returnType, Class<?>[] paramaterTypes, Object[] parameterValue) {
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.paramaterTypes = paramaterTypes;
        this.parameterValue = parameterValue;
    }

}
