package com.appleyk.annotation;

import java.lang.annotation.*;

/**
 * <p>Bean注入注解，仿写@AutoWried</p>
 *
 * @author Appleyk
 * @version V.0.1.1
 * @blob https://blog.csdn.net/Appleyk
 * @date created on 23:58 2020/5/23
 */
// 作用在类中的字段（属性、变量）上。必须的啊，我们也没见过在类上加@AutoWired和方法上加的
@Target(ElementType.FIELD)
// JVM在运行时编译的时候，该注解保留在对应的类中。
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutoWired {
    String value() default "";
}
