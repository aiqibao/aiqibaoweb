package com.aiqibao.demo.mvc.action;
import com.aiqibao.demo.service.IDemoService;
import com.aiqibao.mvcframework.annotation.AQBAutowired;
import com.aiqibao.mvcframework.annotation.AQBController;
import com.aiqibao.mvcframework.annotation.AQBRequestMapping;
import com.aiqibao.mvcframework.annotation.AQBRequestParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @autor aiqibao
 * 2019/9/7 10:46
 * BEST WISH
 */
@AQBController
@AQBRequestMapping("/demo")
public class DemoAction {
    @AQBAutowired private IDemoService demoService ;
    @AQBRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse rsp, @AQBRequestParam("name") String name){
        String result = demoService.get(name) ;
        try {
            rsp.getWriter().write(result);
        }catch(IOException e){
            e.printStackTrace();
        }

    }
    @AQBRequestMapping("/add")
    public void add(HttpServletRequest req,HttpServletResponse rsp,@AQBRequestParam("a") Integer a,@AQBRequestParam("b") Integer b){
        String result = a + "+" + b + "=" + (a + b) ;
        try{
            rsp.getWriter().write(result);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

}
