package com.appleyk.service;

import com.appleyk.annotation.MyService;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>用户服务，提供和用户相关数据操作的业务实现</p>
 *
 * @author Appleyk
 * @version V.0.1.1
 * @blob https://blog.csdn.net/Appleyk
 * @date created on 23:56 2020/5/23
 */

// 这里，我们给user服务bean另起个name，Spring默认注入该bean的name是，类名的首字母小写 == > userService
@MyService("user")
public class UserService {

    public String getUser(String name,String sex){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("你好，欢迎你，"+name +"，sex = "+sex+",当前时间："+df.format(new Date()));
        return "你好，欢迎你，"+name +"，sex = "+sex+",当前时间："+df.format(new Date());
    }

}
