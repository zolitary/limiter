package com.limit.annotation;


import com.limit.enums.LimitEnums;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Limiter {

    //限流的类型
    LimitEnums limitType() default LimitEnums.DEFAULT;

    //限流的key
    String key() default "limiter:";

    //限流的时间
    int time() default 60;

    //限流的次数
    int count() default 5;

}
