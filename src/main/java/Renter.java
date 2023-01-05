import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Renter{


    public void rentHouse() {
        System.out.println("租房成功");
    }

    public static void main(String[] args) {
//        staticProxy staticProxy = new staticProxy(new Renter());
//        staticProxy.rentHouse();
        Renter renter = new Renter();
        DynamicProxy<Renter> personDynamicProxy = new DynamicProxy<>(renter);
        // 这个动态代理接口传进去了接口的class文件，就返回了接口类型的增强后的对象。
        // 因为类可能实现了很多接口，所以Class是一个数组
        Renter renterProxy = (Renter) Proxy.newProxyInstance(Renter.class.getClassLoader(),new Class<?>[]{Renter.class}, personDynamicProxy);
        renterProxy.rentHouse();
    }
}

// 静态代理就是也实现和被代理相同的结构，调用方法的时候改进
// 如果代理方法很多，我们需要在每个代理方法都要写一遍，很麻烦。而动态代理则不需要。
class staticProxy implements Person {
    private Person renter;
    public staticProxy(Person renter) {
        this.renter = renter;
    }

    @Override
    public void rentHouse() {
        System.out.println("中介找房东，转租给租客");
        renter.rentHouse();
        System.out.println("中介给租客钥匙");

    }
}

class DynamicProxy<T> implements InvocationHandler {
    private T target;

    public DynamicProxy(T target) {
        this.target = target;
    }

    // 代理的返回结果，也就是被代理方法的返回结果
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("租客和中介交流");
        Object result = method.invoke(target, args);
        return null;
    }
}
