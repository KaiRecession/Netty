import io.netty.buffer.ByteBuf;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Solution {
    public int a = 1;
    public static void main(String[] args) {
         int[] a = new int[10];
         a[0] = 1;
         a[1] = 2;
         a[3] = 3;
         int[] b = new int[10];
         System.arraycopy(a, 1, b, 1, 3);
         a[1] = 5;
        for (int i = 0; i < b.length; i++) {
            System.out.print(b[i]);
        }
    }
    void run() {
        int a = 0;
        System.out.println(a);
    }
    private void run2() {
        System.out.println(11);
    }
    private <T> void run3(T t) {
        
    }
}
class a extends Solution {
    public void run2() {
//        super.run2();
        System.out.println(22);
    }

    public a() {
    }
}
