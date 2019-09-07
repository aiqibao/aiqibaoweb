package com.aiqibao.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @autor aiqibao
 * 2019/9/7 10:43
 * BEST WISH
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AQBRequestParam {
    String value() default "" ;
}
