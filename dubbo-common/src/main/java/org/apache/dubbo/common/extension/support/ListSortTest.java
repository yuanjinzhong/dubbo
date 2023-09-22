package org.apache.dubbo.common.extension.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
 public class ListSortTest {
    public static void main(String[] args) {
        // 创建一个包含整数的列表
        List<Integer> numbers = new ArrayList<>();
        numbers.add(5);
        numbers.add(2);
        numbers.add(8);
        numbers.add(1);
        numbers.add(3);
         // 使用Comparator进行降序排序
        numbers.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer num1, Integer num2) {
                return  num1 > num2 ? 1 : -1;
                //return num2.compareTo(num1);
            }
        });
         // 打印排序后的列表
        System.out.println("降序排序后的列表：");
        for (Integer number : numbers) {
            System.out.println(number);
        }
    }
}
