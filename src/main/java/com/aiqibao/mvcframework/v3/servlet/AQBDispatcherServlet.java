package com.aiqibao.mvcframework.v3.servlet;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @autor aiqibao
 * 2019/9/7 22:52
 * BEST WISH
 */
public class AQBDispatcherServlet extends HttpServlet {

    private static final String LOCALTION_CONFIG = "aqbConfig" ;
    private Properties configProperties = new Properties() ;
    private List<String> classNames = new ArrayList<String>();
    private Map<String,Object> ioc = new HashMap<String,Object>();
    private List<Handle> handleMappings = new ArrayList<>() ;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            this.doDispatch(req,resp) ;
        }catch (Exception e){
            e.printStackTrace();
            resp.getWriter().write("500 Exception" + e.getMessage());
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        Handle handle = getHandle(req) ;
        try{
            if (handle == null){
                resp.getWriter().write("404 Not Found");
                return ;
            }

            //获取方法的参数列表
            Class<?>[] paramType =handle.method.getParameterTypes();

            //保存所有需要自动赋值的参数值
            Object[] paramValues = new Object[paramType.length] ;
            Map<String,String[]> params = req.getParameterMap() ;
            for (Map.Entry<String,String[]> param:params.entrySet()){
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
                               .replaceAll(",\\s",",");
                //如果找到匹配的对象，则开始填充参数值
                if (!handle.paramIndexMapping.containsKey(param.getKey())){continue;}
                int index = handle.paramIndexMapping.get(param.getKey());
                paramValues[index] =convert(paramType[index],value);

                //设置方法中的request和response对象
                int reqIndex = handle.paramIndexMapping.get(HttpServletRequest.class.getName());
                paramValues[reqIndex] = req;
                int rspIndex = handle.paramIndexMapping.get(HttpServletResponse.class.getName());
                paramValues[rspIndex] = resp ;
                handle.method.invoke(handle.controller,paramValues);

            }
        }catch (Exception e){

        }

    }

    public Object convert(Class<?> type,String value){
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        return  value ;
    }

    private Handle getHandle(HttpServletRequest req) {
        if(this.handleMappings.isEmpty()){return  null;}
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");
        for (Handle handle:handleMappings){
            Matcher matcher =handle.pattern.matcher(url) ;
            if (!matcher.matches()){continue;}
            return handle;
        }
        return null ;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件
        doLoadConfigProperties(config.getInitParameter(LOCALTION_CONFIG)) ;
        //2、扫描所有相关的类
        doScanner(configProperties.getProperty("scanPackage")) ;
        //3、初始化所有类的实例，并保存到IOC中
        doInstance();
        //4、依赖注入
        doAutoWired();
        //5、构造HandleMapping
        initHandleMapping();

        //6、等待请求

        System.out.println("AQB springmvcframework is init");
    }

    private void initHandleMapping() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass() ;
            if (!clazz.isAnnotationPresent(AQBController.class)){continue;}
            //获取Controller的url配置
            String url = "" ;
            if (clazz.isAnnotationPresent(AQBRequestMapping.class)){
                AQBRequestMapping requestMapping = clazz.getAnnotation(AQBRequestMapping.class);
                url = requestMapping.value();
            }
            //获取method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method:methods){
                if(!method.isAnnotationPresent(AQBRequestMapping.class)){continue;}
                //映射url
                AQBRequestMapping requestMapping = method.getAnnotation(AQBRequestMapping.class);
                String regex = ("/"+url+requestMapping.value()).replaceAll("/+","/");
                Pattern pattern = Pattern.compile(regex) ;
                handleMappings.add(new Handle(entry.getValue(),method,pattern));
                System.out.println("mapping " + regex + "," + method);
            }
        }
    }

    private void doAutoWired() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            //拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if (!field.isAnnotationPresent(AQBAutowired.class)){continue;}
                AQBAutowired autowired = field.getAnnotation(AQBAutowired.class);
                String beanName = autowired.value();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true); //设置私有属性的访问权限
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()){return;}
        try {
            for (String className:classNames){
                Class<?> clazz = Class.forName(className) ;
                if (clazz.isAnnotationPresent(AQBController.class)){
                    String beanName = LowFirst(clazz.getSimpleName());
                    ioc.put(beanName,clazz.newInstance()) ;
                }else if(clazz.isAnnotationPresent(AQBService.class)){
                    AQBService aqbService = clazz.getAnnotation(AQBService.class) ;
                    String beanName = aqbService.value();
                    if(!"".equals(beanName)){
                        ioc.put(beanName,clazz.newInstance()) ;
                        continue;
                    }
                    Class<?>[] classInters = clazz.getInterfaces() ;
                    for (Class classInter:classInters){
                        ioc.put(classInter.getName(),clazz.newInstance());
                    }
                }else{continue;}
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.","/"));
        File files = new File(url.getFile());
        for (File file:files.listFiles()){
            //如果是文件夹，则继续递归
            if (file.isDirectory()){
                doScanner(scanPackage +"."+ file.getName());
            }else{
                classNames.add(scanPackage + "." + file.getName().replaceAll(".class","").trim());
            }

        }
    }

    private void doLoadConfigProperties(String aqbConfig) {
        InputStream is = null ;
        is = this.getClass().getClassLoader().getResourceAsStream(aqbConfig) ;
        try {
            configProperties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(is!= null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String LowFirst(String simpleName){
        char[] chars = simpleName.toCharArray() ;
        chars[0] += 32 ;
        return String.valueOf(chars);
    }

    public class Handle{
        protected Object controller ; //保存方法对应的实例
        protected Method method; //保存映射的方法
        protected Pattern pattern ;
        protected Map<String,Integer> paramIndexMapping;//保存参数顺序

        public Handle(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<String ,Integer>() ;
            putParamIndexMapping(method) ;
        }

        private void putParamIndexMapping(Method method) {
            //提取方法中加了注解的参数
            Annotation[][] pa = method.getParameterAnnotations() ;
            for (int i = 0; i < pa.length ; i++) {
                 for(Annotation a:pa[i]){
                     if (a instanceof AQBRequestParam){
                         String paramName = ((AQBRequestParam) a).value();
                         if (!"".equals(paramName.trim())){
                             paramIndexMapping.put(paramName,i);
                         }
                     }
                 }
            }

            //提取方法中的request和response参数
            Class<?>[] paramType = method.getParameterTypes() ;
            for (int i = 0; i <paramType.length ; i++) {
                Class type = paramType[i];
                if (type==HttpServletRequest.class||type==HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);
                }
            }
        }
    }
}
