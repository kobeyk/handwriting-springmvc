package com.appleyk.annotation;

import java.lang.annotation.*;

/**
 * <p>请求Mapping注解，仿写@RequestMapping</p>
 *
 * @author Appleyk
 * @version V.0.1.1
 * @blob https://blog.csdn.net/Appleyk
 * @date created on 23:58 2020/5/23
 */
// 作用在类或方法上，必须的啊，类上配置根url，方法上配置子url
@Target({ElementType.TYPE,ElementType.METHOD})
// JVM在运行时编译的时候，该注解保留在对应的类中。
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value() default "";
}
