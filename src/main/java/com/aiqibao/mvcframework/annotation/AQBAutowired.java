package com.aiqibao.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @autor aiqibao
 * 2019/9/7 10:16
 * BEST WISH
 */
@Target({ElementType.FIELD})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface AQBAutowired {
    String value() default "" ;
}
