package com.aiqibao.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @autor aiqibao
 * 2019/9/7 10:39
 * BEST WISH
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AQBRequestMapping {
    String value() default "";
}
