package org.apache.dubbo.demo.consumer;

import javassist.*;

/**
 * javaasist动态生成类
 */
public class DynamicClassGenerator {

    /**
     * 不能有两个main 方法， Application 里面也有个main
     * @param args
     * @throws Exception
     */
    public static void main2(String[] args) throws Exception {
        // 创建ClassPool对象，用于管理生成的类
        ClassPool pool = ClassPool.getDefault();

        // 创建一个新的Class对象
        CtClass dynamicClass = pool.makeClass("DynamicClass");

        // 添加字段
        CtField field = CtField.make("private String name;", dynamicClass);
        dynamicClass.addField(field);

        // 添加方法
        CtMethod method = CtMethod.make("public String getName() { return name; }", dynamicClass);
        dynamicClass.addMethod(method);

        // 添加set方法
        CtMethod ctMethod = CtMethod.make("public void setName(String name) { this.name=name;}", dynamicClass);
        dynamicClass.addMethod(ctMethod);

        // 生成类文件
        dynamicClass.writeFile();

        // 加载生成的类并创建实例
        Class<?> clazz = dynamicClass.toClass();
        Object instance = clazz.newInstance();

        // 调用方法并输出结果
        clazz.getMethod("setName", String.class).invoke(instance, "John Doe");
        String name = (String) clazz.getMethod("getName").invoke(instance);
        System.out.println("Name: " + name);
    }
}
