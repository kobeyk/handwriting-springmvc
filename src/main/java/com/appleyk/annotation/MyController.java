package com.appleyk.annotation;

import java.lang.annotation.*;

/**
 * <p>类控制器注解，仿写@Controller</p>
 *
 * @author Appleyk
 * @version V.0.1.1
 * @blob https://blog.csdn.net/Appleyk
 * @date created on 23:58 2020/5/23
 */
// 作用在类上，必须的啊，控制器必须是类
@Target(ElementType.TYPE)
// JVM在运行时编译的时候，该注解保留在对应的类中。
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {
    String value() default "";
}
