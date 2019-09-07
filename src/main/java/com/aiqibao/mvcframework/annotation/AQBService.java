package com.aiqibao.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @autor aiqibao
 * 2019/9/7 10:44
 * BEST WISH
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AQBService {
    String value() default "" ;
}
