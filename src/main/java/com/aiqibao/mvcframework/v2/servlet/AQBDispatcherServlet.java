package com.aiqibao.mvcframework.v2.servlet;

import com.aiqibao.mvcframework.annotation.*;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @autor aiqibao
 * 2019/9/7 14:00
 * BEST WISH
 */
public class AQBDispatcherServlet extends HttpServlet {

    private Map<String, Method> handleMapping = new HashMap<String,Method>() ;

    private List<String> classNames = new ArrayList<>() ;

    private Properties configProperties = new Properties() ;

    private Map<String,Object> ioc = new HashMap<String,Object>() ;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //派遣  分发任务
        try{
            //委派模式
            doDispatch(req,resp);
        }catch (Exception e){
            e.printStackTrace();
            resp.getWriter().write("500 Exception " + Arrays.toString(e.getStackTrace()) );
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、加载配置文件
        doLoadConfig(config.getInitParameter("aqbConfig")) ;
        //2、扫描相关的类
        doScanner(configProperties.getProperty("scanPackage")) ;
        //3、初始化所有相关的类的实例，并且放入到IOC容器之中
        doInstance();
        //4、完成依赖注入
        doAutowried();
        //5、初始化HandleMapping
        initHandleMapping();
        System.out.println("AQB springFramework is init");
    }

    private void initHandleMapping() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass() ;
            if(!clazz.isAnnotationPresent(AQBController.class)){continue;}
            String baseUrl = "" ;
            //获得Controller的url配置
            if(clazz.isAnnotationPresent(AQBRequestMapping.class)){
                AQBRequestMapping requestMapping = clazz.getAnnotation(AQBRequestMapping.class);
                baseUrl = requestMapping.value() ;
            }

            //获取Method的url配置
            Method[] methods =clazz.getMethods() ;
            for (Method method:methods){
                //没有加Requesmapping注解的直接忽略
                if (!method.isAnnotationPresent(AQBRequestMapping.class)){continue;}
                //映射URL
                AQBRequestMapping requestMapping = method.getAnnotation(AQBRequestMapping.class) ;
                String url = ("/" + baseUrl + "/" + requestMapping.value())
                             .replaceAll("/+","/");
                handleMapping.put(url,method);
                System.out.println("Mapped " + url + "," + method);
            }
        }
    }

    private void doAutowried() {
        if(ioc.isEmpty()){return;}
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            //拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if (!field.isAnnotationPresent(AQBAutowired.class)){continue;}
                AQBAutowired aqbAutowired = field.getAnnotation(AQBAutowired.class) ;
                String beanName = aqbAutowired.value().trim() ;
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    //控制反转过程
    //工厂模式来实现
    private void doInstance() {
        if(classNames.isEmpty()){return;}
        for (String className:classNames){
            try{
                Class<?> clazz = Class.forName(className) ;
                if (clazz.isAnnotationPresent(AQBController.class)){
                    Object instance = clazz.newInstance() ;
                    String beanName = toLowerFirstCase(clazz.getSimpleName()) ;
                    ioc.put(beanName,instance);
                }else if(clazz.isAnnotationPresent(AQBService.class)){
                    String beanName = toLowerFirstCase(clazz.getSimpleName()) ;
                    AQBService aqbService = clazz.getAnnotation(AQBService.class);
                    if (!"".equals(aqbService.value())){
                        beanName = aqbService.value() ;
                    }
                    Object instance = clazz.newInstance() ;
                    ioc.put(beanName,instance) ;
                    for (Class<?> i : clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The BeanName is exists") ;
                        }
                        ioc.put(i.getName(),instance) ;
                    }
                }else{continue;}
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private void doScanner(String scanPackage) {
        //包传过来包下面所有的类全部扫描出来
        URL url = this.getClass().getClassLoader()
                  .getResource("/"+scanPackage.replaceAll("\\.","/"));
        File files = new File(url.getFile()) ;
        for (File file:files.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                if(!file.getName().endsWith(".class")){continue;}
                String className = (scanPackage + "." + file.getName().replace(".class",""));
                classNames.add(className) ;
            }
        }
    }

    private void doLoadConfig(String aqbConfig) {
        InputStream is = null ;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(aqbConfig) ;
            configProperties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void doDispatch(HttpServletRequest req,HttpServletResponse rsp) throws Exception{
        String url = req.getRequestURI() ;
        String contextPath =req.getContextPath() ;
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        if(!this.handleMapping.containsKey(url)){
            rsp.getWriter().write("404 Not Found");
            return;
        }
        Method method = this.handleMapping.get(url) ;
        //第一个参数：方法所在的实例
        //第二个参数：调用时所需要的实参
        Map<String,String[]> params = req.getParameterMap() ;
        //获取方法的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes() ;
        //保存请求的url参数列表
        Map<String,String[]> parameterMap = req.getParameterMap() ;
        //保存赋值参数的位置
        Object[] paramValues = new Object[parameterTypes.length] ;
        //按根据参数位置动态赋值
        for (int i = 0; i < parameterTypes.length; i++) {
            Class paramerType = parameterTypes[i] ;
            if(paramerType == HttpServletRequest.class){
                paramValues[i] = req ;
                continue;
            }else if(paramerType == HttpServletResponse.class){
                paramValues[i] = rsp ;
                continue;
            }else if(paramerType == String.class){
                //提取方法中加了注解的参数
                Annotation[][] pa = method.getParameterAnnotations() ;
                for (int j = 0; j < pa.length; j++) {
                    for(Annotation a:pa[i]){
                        if(a instanceof AQBRequestParam){
                            String paramName = ((AQBRequestParam) a).value() ;
                            if("".equals(paramName.trim())){
                                String value = Arrays.toString(parameterMap.get(paramName))
                                               .replaceAll("\\[|\\]","")
                                               .replaceAll("\\s",",") ;
                                paramValues[i] = value ;
                            }
                        }
                    }
                }
            }
        }
        //投机取巧的方式
        //通过反射拿到method所在的class，拿到class之后还是拿到calss的名称
        //再调用toLowerFirstCase获得beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),new Object[]{req,rsp,params.get("name")[0]});
    }

    private String toLowerFirstCase(String simpleName){
        char[] chars = simpleName.toCharArray() ;
        chars[0] += 32 ;
        return String.valueOf(chars) ;
    }
}
