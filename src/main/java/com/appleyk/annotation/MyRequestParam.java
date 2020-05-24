package com.appleyk.annotation;

import java.lang.annotation.*;

/**
 * <p>请求参数注解，仿写@RequestParam</p>
 *
 * @author Appleyk
 * @version V.0.1.1
 * @blob https://blog.csdn.net/Appleyk
 * @date created on 23:58 2020/5/23
 */
// 作用在Controller类中Method的参数上
@Target(ElementType.PARAMETER)
// JVM在运行时编译的时候，该注解保留在对应的类中。
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {
    String value() default "";
}
