package com.aiqibao.demo.service.impl;

import com.aiqibao.demo.service.IDemoService;
import com.aiqibao.mvcframework.annotation.AQBService;

/**
 * @autor aiqibao
 * 2019/9/7 10:46
 * BEST WISH
 */
@AQBService
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is " + name + " DemoService";
    }
}
