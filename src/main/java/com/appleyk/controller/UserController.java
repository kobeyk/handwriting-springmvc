package com.appleyk.controller;

import com.appleyk.annotation.MyAutoWired;
import com.appleyk.annotation.MyController;
import com.appleyk.annotation.MyRequestMapping;
import com.appleyk.annotation.MyRequestParam;
import com.appleyk.service.UserService;

/**
 * <p>用户请求控制器</p>
 *
 * @author Appleyk
 * @version V.0.1.1
 * @blob https://blog.csdn.net/Appleyk
 * @date created on 0:07 2020/5/24
 */

@MyController
@MyRequestMapping("/user")
public class UserController {

    /**这里，我们不走Spring默认的bean名称，而是指定要注入beanName = user的类实例对象*/
//    @MyAutoWired()
    @MyAutoWired("user")
    private UserService userService;

    @MyRequestMapping("/query")
    public String query(@MyRequestParam("name") String name ,@MyRequestParam("sex") String sex){
        /**
         * 1.1 注意，如果手写SpringMVC，不处理userService的注入问题，则下面这段执行，必然会报空指针
         * 1.2 因为，你只是告诉这个Controller类，要注入哪个bean，没有说bean在哪拿啊，
         * 1.3 所以，这种光说不做的行为，就是耍流氓！！！一定要注意！！！
         */
        return userService.getUser(name,sex);
    }

}
