package org.apache.dubbo.common.extension.support;

import java.util.Comparator;
 public class ComparatorTest {
    public static void main(String[] args) {
        // 创建一个自定义的Comparator对象
        Comparator<String> stringComparator = new Comparator<String>() {
            @Override
            public int compare(String str1, String str2) {
                // 根据字符串长度比较
                return Integer.compare(str1.length(), str2.length());
            }
        };
         // 测试compare方法
        String str1 = "apple";
        String str2 = "banana";
        int result = stringComparator.compare(str1, str2);
        System.out.println("Compare result: " + result);

         // 测试reversed方法
        Comparator<String> reversedComparator = stringComparator.reversed();
        int reversedResult = reversedComparator.compare(str1, str2);
        System.out.println("Reversed compare result: " + reversedResult);
    }
}
