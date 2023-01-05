public class Test {
    public static void main(String[] args) {
        Object test1 = (Object) new Test1();
        test1 = (Test) test1;

    }
    public void run() {
        System.out.println("test的run");
    }
}

class Test1{
    public void run() {
        System.out.println("test1的run");
    }
}
