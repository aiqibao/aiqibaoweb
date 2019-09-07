package com.aiqibao.mvcframework.v1.servlet;

import com.aiqibao.mvcframework.annotation.AQBAutowired;
import com.aiqibao.mvcframework.annotation.AQBController;
import com.aiqibao.mvcframework.annotation.AQBRequestMapping;
import com.aiqibao.mvcframework.annotation.AQBService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @autor aiqibao
 * 2019/9/7 11:04
 * BEST WISH
 */
public class AQBDispatcherServlet extends HttpServlet {

    private Map<String,Object>  mapping = new HashMap<String,Object>() ;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            doDispatch(req,resp);
        }catch (Exception e){
            resp.getWriter().write("500 exception" + e.getStackTrace());
        }

    }

    public void doDispatch(HttpServletRequest req,HttpServletResponse rsp) throws Exception{
        String url = req.getRequestURI();
        System.out.println("url" + url);
        String contextPath = req.getContextPath() ;
        System.out.println("contextPath:"+contextPath);
        url = url.replace(contextPath,"").replaceAll("/+","/");
        if(!this.mapping.containsKey(url)){rsp.getWriter().write("404 not found");return;}
        Method method = (Method) this.mapping.get(url);
        Map<String,String[]> params = req.getParameterMap() ;
        method.invoke(this.mapping.get(method.getDeclaringClass().getName()),new Object[]{req,rsp,params.get("name")[0]});
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        InputStream is = null ;
        try{
            Properties configContext = new Properties() ;
            is = this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter("aqbConfig")) ;
            configContext.load(is);
            String scnnerPackage =configContext.getProperty("scanPackage") ;
            doScanner(scnnerPackage);
            for(String className:mapping.keySet()){
                if(!className.contains(".")){continue;}
                Class<?> clazz = Class.forName(className) ;
                if(clazz.isAnnotationPresent(AQBController.class)){
                    mapping.put(className,clazz.newInstance()) ;
                    String baseUrl ="" ;
                    if(clazz.isAnnotationPresent(AQBRequestMapping.class)){
                        AQBRequestMapping requestMapping = clazz.getAnnotation(AQBRequestMapping.class) ;
                        baseUrl = requestMapping.value() ;
                    }
                    Method[] methods = clazz.getMethods() ;

                    for(Method method:methods){
                        if(!method.isAnnotationPresent(AQBRequestMapping.class)){continue;}
                        AQBRequestMapping requestMapping = method.getAnnotation(AQBRequestMapping.class) ;
                        String url = (baseUrl +  "/" + requestMapping.value()).replaceAll("/+","/") ;
                        mapping.put(url,method) ;
                        System.out.println("Mapped" + url + "," + method);
                    }
                }else if (clazz.isAnnotationPresent(AQBService.class)){
                    AQBService service = clazz.getAnnotation(AQBService.class) ;
                    String beanName = service.value() ;
                    if("".equals(beanName)){beanName = clazz.getName();}
                    Object instance = clazz.newInstance() ;
                    mapping.put(beanName,instance) ;
                    for (Class<?> i:clazz.getInterfaces()){
                        mapping.put(i.getName(),instance) ;
                    }
                }else{continue;}
            }
            for(Object obj:mapping.values()){
                if (obj == null){continue;}
                Class clazz =obj.getClass()  ;
                if(clazz.isAnnotationPresent(AQBController.class)){
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field:fields){
                         if(!field.isAnnotationPresent(AQBAutowired.class)){continue;}
                         AQBAutowired aqbAutowired = field.getAnnotation(AQBAutowired.class) ;
                         String beanName = aqbAutowired.value();
                         if("".equals(beanName)){beanName=field.getType().getName();}
                         field.setAccessible(true);
                         field.set(mapping.get(clazz.getName()),mapping.get(beanName));
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(is != null){
                try {
                    is.close();
                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        }
    }

    private void doScanner(String scannerPackage){
        URL url = this.getClass().getClassLoader().getResource("/"+scannerPackage.replaceAll("\\.","/"));
        System.out.println("url:" + url);
        File classDir = new File(url.getFile()) ;
        for (File file:classDir.listFiles()){
            if(file.isDirectory()) {doScanner(scannerPackage + "." + file.getName());}else{
                if(!file.getName().endsWith(".class")){continue;}
                String clazzName = (scannerPackage + "." + file.getName().replace(".class",""));
                mapping.put(clazzName,null) ;
            }
        }

    }
}
