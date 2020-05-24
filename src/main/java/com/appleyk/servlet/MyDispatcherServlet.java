package com.appleyk.servlet;

import com.appleyk.annotation.*;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>自定义前置控制器/p>
 *
 * @author Appleyk
 * @version V.0.1.1
 * @blob https://blog.csdn.net/Appleyk
 * @date created on 0:23 2020/5/24
 */
public class MyDispatcherServlet extends HttpServlet {

    // 重写三个方法，一个是父类GenericServlet的初始化方法init()，两个分别是父类HttpServlet的doGet和doPost请求处理方法

    /**存放指定的扫描包下面的所有的class*/
    List<String> classNames = new ArrayList<>();

    /**存放符合条件的实例化后的bean（类似于简版的ioc容器），如加了xxx注解的bean要放进来*/
    Map<String,Object> beanMap = new HashMap<>(16);

    /**url和method映射关系 （请求api地址和处理请求的方法的键值对）*/
    Map<String, Method> urlMethodMap = new HashMap<>(16);

    @Override
    public void init() throws ServletException {
        // 1、第一步，扫描指定的包（主要是扫描xxx.class文件）
        scanPackage("com.appleyk");
        System.out.println("classNames = " + classNames);
        // 2、利用反射，实例化bean（将实例化的bean，放入bean容器map中）
        beanInstance();
        System.out.println("beanMap = " + beanMap);
        //3、处理@XXAutoWired注解，bean实例中的字段如果有这个注解（依赖其他bean），则将该注解对应的实例从beanMap中取出，并赋予该字段
        doAutoWired();
        // 4、处理请求的url与控制器类中方法的mapping（映射）关系
        handleMapping();
        System.out.println("urlMethodMap = " + urlMethodMap);
    }


    /**
     * 处理带@XXAutoWired注解的类字段，将字段赋值（其实就是实现一个bean依赖另一个bean实例，完成依赖bean的注入）
     * 实现技术：反射
     */
    public void doAutoWired() {

        if(beanMap.size() == 0){
            return;
        }
        for (Map.Entry<String,Object> entry : beanMap.entrySet()){
            // 1、遍历拿到bean实例
            Object beanInstance = entry.getValue();
            // 2、获取bean的class
            Class<?> classz = beanInstance.getClass();
            // 3、通过反射，拿到class中所有的字段（private、public、protected,但不包含父类的）
            Field[] declaredFields = classz.getDeclaredFields();
            if(declaredFields == null || declaredFields.length == 0){
                continue;
            }
            // 4、遍历字段，看下字段是不是含有@XXAutoWired注解,没有的话，就跳过
            for (Field declaredField : declaredFields) {
                if(!declaredField.isAnnotationPresent(MyAutoWired.class)){
                    continue;
                }
                // 4.1 有的话，拿到AutoWired的value
                MyAutoWired autoWired = declaredField.getAnnotation(MyAutoWired.class);
                String wriedBeanName = autoWired.value();
                // 4.2 如果AutoWired的value（其实就是注入bean的name）等于空，那就拿字段对应的类的类型
                if("".equals(wriedBeanName)){
                    // 4.2.1 这个拿到的是类的完全限定名称，就是xxx.xxx.xxxClass
                    String declaredFieldClassType = declaredField.getType().getName();
                    try{
                        // 4.2.2 通过反射，拿到字段对应的类
                        Class<?> declaredFieldClassz = Class.forName(declaredFieldClassType);
                        // 4.2.3 获取类的名称
                        String fieldSimpleName = declaredFieldClassz.getSimpleName();
                        // 4.2.4 Spring默认bean的name是类名的首字母小写，下面转换一下（考虑问题细致一些）
                        wriedBeanName = lowerFirstCase(fieldSimpleName);
                    }catch (ClassNotFoundException ex){
                        ex.printStackTrace();
                    }
                }

                System.out.println("带有注解XXAutoWired的类字段的类型（beanName）= " +wriedBeanName);
                /**
                 * 说明：上一步小复杂的操作，其实就是要定位到，是who使用了注入功能，这个who的name我们已经找到
                 *     接着，就是拿到这个who的name（key），去beanMap（bean容器）中拿到对应的instance（value）了
                 * 插曲：注意，我们在类中的字段who的修饰符通常都是private的，如果给字段赋值（完成注入）
                 *      则需要，首先让这个字段变成"public"公开的，所以，直接看下面操作
                 */
                // 4.3 先判断下，field的bean实例在不在bean容器中，如果不在，还玩个锤子啊，肯定提示异常啊
                if(!beanMap.containsKey(wriedBeanName)){
                    // 注意 如果@AutoWired("xxxx")中的xxx指定错了，就会报bean找不到的信息
                    // 这个应该是很好理解的，因为你随便注入一个不存在IOC容器中的bean进来，你叫IOC给你上哪去造啊
                    throw new NullPointerException(wriedBeanName+"找不到对应的bean，注入失败！");
                }
                try{
                    // 4.4 放开设置的权限
                    declaredField.setAccessible(true);
                    // set(Object,Object),第一个参数，是这个field属于那个类，第二个是，要给这个field设置哪个对象
                    // 4.5 字段赋值（把从bean容器中拿到的instance，赋给该字段，完成类的依赖bean的注入）
                    declaredField.set(beanInstance,beanMap.get(wriedBeanName));
                    /**至此，完成一个注入field的处理，然后就是可以愉快的在类中使用field实例了*/
                }catch (IllegalAccessException ex){
                    ex.printStackTrace();
                }
            }
        }

    }

    /**
     * 处理带@XXRequestMapping注解的类或方法，拼接请求url，并将url和method的映射关系记录下来
     * 实现技术：反射
     */
    public void handleMapping() throws ServletException{

        String apiUrl = "";
        /**这个方法要干的事情，依然是上来先遍历bean容器中的class实例，当然实例是要过滤的，为什么呢，留个念想*/
        for (Object beanInstance : beanMap.values()) {
            // 1、只保留带有@XXController注解的类，因为Service类也处理不了url请求啊
            //    （当然这种说法，是按规矩来的，不按的话，我们可以将@XXService注解和@XXController注解的功能换一下）
            Class<?> classz = beanInstance.getClass();
            if(!classz.isAnnotationPresent(MyController.class)){
                // 1.1 其他非Controller注解的类，跳过，不处理
                continue;
            }
            // 1.2 如果有的话，先拿到类上的RequestMapping的url
            if(classz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping classRM = classz.getAnnotation(MyRequestMapping.class);
                // 1.2.1 获取value值，也就是url
                String urlClass = classRM.value();
                // 1.2.2 拿到类上的url后，我们就要遍历方法了，获取方法上的url
                // 通过反射，拿到本类中和父类中所有public的methods，注意这里不要用getDeclaredMethods()
                Method[] methods = classz.getMethods();
                // 1.2.3 开始遍历
                for (Method method : methods) {
                    // 1.2.3.1 看下，方法是行不是含有@XXRequestMapping(url)注解
                    if(!method.isAnnotationPresent(MyRequestMapping.class)){
                        // 如果public方法不含的话，跳过，不处理
                        continue;
                    }
                    // 1.2.3.2 有的话，当然是拿到url地址了
                    MyRequestMapping methodRM = method.getAnnotation(MyRequestMapping.class);
                    String urlMethod = methodRM.value();
                    // 1.2.3.3 拼接完整的请求url
                    apiUrl = urlClass+urlMethod;
                    // 1.2.3.4 下面要干一件事情，url是全局唯一的，Spring不允许相同的url出现两次,所以做下判断
                    if(urlMethod.contains(apiUrl)){
                        throw new ServletException("apiUrl = "+apiUrl+",已经存在了，请核查保证url唯一！");
                    }
                    // 1.2.3.4 将url和method的映射关系存起来
                    urlMethodMap.put(apiUrl,method);
                }
            }
        }
    }

    /**
     * 通过反射，实例化类，通过类上的注解，过滤哪些类是需要放入bean容器中的
     */
    public void beanInstance(){
        if(classNames.size() == 0){
            return;
        }
        for (String className : classNames) {
            try {
                // 1、根据类的限定名获取class
                Class<?> classz = Class.forName(className);
                // 2、定义类的实例
                Object instance = null;
                // 2、判断类是否有xxController、xxService注解
                if(classz.isAnnotationPresent(MyController.class)){
                    // 2.1 获取MyController注解的value，拿到bean的名称
                    MyController annotation = classz.getAnnotation(MyController.class);
                    // 2.2 根据反射，拿到class的实例
                    instance = classz.newInstance();
                    beanMap.put(annotation.value().equals("") ? lowerFirstCase(classz.getSimpleName()) : annotation.value(),instance);
                }else if(classz.isAnnotationPresent(MyService.class)){
                    // 3.1 获取MyService注解的value，拿到bean的名称
                    MyService annotation = classz.getAnnotation(MyService.class);
                    // 3.2 根据反射，拿到class的实例
                    instance = classz.newInstance();
                    beanMap.put(annotation.value().equals("") ? lowerFirstCase(classz.getSimpleName()) : annotation.value(),instance);
                }else{
                    // 其他注解，不管，直接跳过，进入下一个
                    continue;
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 获取指定扫描包下面的所有class的名称（限定类名）
     * @param packageStr 要扫描的包名
     */
    public void scanPackage(String packageStr){

        // 1、将xx.xx结构的包名，转换为实际意义上的xx/xx/路径
        String pageckageDir = packageStr.replaceAll("\\.", "/");
        // 2、根据包路径，拿到当前类路径（classPath）
        URL resource = this.getClass().getClassLoader().getResource(pageckageDir);
        // 3、拿到资源的文件（全）路径
        String fileStr = resource.getFile();
        // 4、根据路径创建文件
        File file = new File(fileStr);
        // 5、判断文件是否存在，不存在直接返回
        if(!file.exists()){
            return;
        }

        // 6、获取classes文件下面的所有文件集合（可能是文件，也有可能是目录）
        File[] files = file.listFiles();
        for (File filePath : files) {
            String className = packageStr + "." + filePath.getName().replace(".class","");
            if(filePath.isDirectory()){
                // 如果是文件夹的话，递归继续获取类限定名
                scanPackage(className);
            }else{
                // 如果是文件的话，直接包名+“.”+文件拼接成类限定名,然后加入到集合中
                classNames.add(className);
            }
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1、获取request中的url
        String uri = req.getRequestURI();
        System.out.println(uri);
        // 2、获取到项目路径（即tomcat配置部署的项目名）
        String contextPath = req.getContextPath();
        // 3、处理下uri，将uri中的项目名（部署在tomcat中war的名称）剔除
        uri = uri.replace(contextPath,"");
        // 4、通过url，找到对应的方法
        if(!urlMethodMap.containsKey(uri)){
            System.out.println("uri无效，请求404！");
        }
        Method method = urlMethodMap.get(uri);
        System.out.println(method.getName());
        // 5、拿到方法对应的类（方法名不是唯一，只有确定了所在类，后面才能调用invoke方法）
        Class<?> declaringClass = method.getDeclaringClass();
        System.out.println("Method所在的类 : "+ declaringClass);
        try {
            // 5.1 基于method所在的类，获取类的simpleName,然后从beanMap（IOC）容器中拿method所在类的实例
            //     之所以要从bean容器中拿，是因为，bean容器的实例，是经过依赖注入过的，也就是doAutoWried过的
            //     当然，如果controller的beanName走的不是默认，下面的处理方式就需要做改变了
            Object beanInstance = beanMap.get(lowerFirstCase(declaringClass.getSimpleName()));
            // 5.2 获取method的参数
            Parameter[] params = method.getParameters();
            // 5.3 判断下，如果method没有参数的话，直接调用不带参数的invoke
            if(params == null || params.length == 0){
                method.invoke(beanInstance);
                return;
            }
            // 5.4 存放参数的name,索引作为key（注意，方法invoke的时候，参数是有顺序的）
            Map<Integer,String> paramMap = new HashMap<>();
            int paramIndex = 0 ;
            for (Parameter param : params) {
                if(!param.isAnnotationPresent(MyRequestParam.class)){
                    continue;
                }
                MyRequestParam paramAnnotation = param.getAnnotation(MyRequestParam.class);
                String paramName = paramAnnotation.value();
                System.out.println("参数 【paramName】 = " + paramName);
                paramMap.put(paramIndex++,paramName);
            }
            // 5.5 基于method的参数name列表，构造参数值的数组
            Object[] paramObjs = new Object[paramMap.size()];
            // 5.6 给参数赋值之前，先来遍历下request中的参数，如果用户前端发起的请求中的参数name在后台没有给定时，报错
            if(paramMap!=null && paramMap.size()>0){
                for (Map.Entry<String, String[]> paramEntry : req.getParameterMap().entrySet()) {
                    String key = paramEntry.getKey();
                    if(!paramMap.values().contains(key)){
                        System.out.println("参数"+key+"无效，找不到与之对应的名称！");
                        throw new ServletException("参数"+key+"无效，找不到与之对应的名称！");
                    }
                }
            }
            // 5.7 遍历参数map，结合req的参数，进行赋值操作（如果前端没传参数，则后台给参数默认值）
            for (Map.Entry<Integer, String> entry : paramMap.entrySet()) {
                // 给参数赋值，如果req传进来的参数等于空的话，就给默认值
                paramObjs[entry.getKey()] = req.getParameter(
                        entry.getValue()) == null ? "默认值，这个值最好放在自定义的注解里" : req.getParameter(entry.getValue());
            }

            // 5.8 倒数第二步，一切准备就绪，那就是invoke调用了
            Object result = method.invoke(beanInstance, paramObjs);
            // 5.9 最后一步，方法调用后，拿到调用的结果，然后往resp对象里写入，输出到浏览器端
            //     正常的来说，应该是获取method的返回类型，根据类型来确定写入os流中的数据
            ServletOutputStream outputStream = resp.getOutputStream();
            outputStream.write(result.toString().getBytes());

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    /**
     * 首字母小写
     */
    private String lowerFirstCase(String str){
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
