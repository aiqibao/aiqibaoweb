package com.aiqibao.demo.service.impl;

import com.aiqibao.demo.service.IDemoService;

/**
 * @autor aiqibao
 * 2019/9/7 10:46
 * BEST WISH
 */
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is " + name ;
    }
}
