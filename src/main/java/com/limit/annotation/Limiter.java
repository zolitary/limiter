package com.limit.annotation;
import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Limiter {

    //使用SpEL表达式获取redisKey
    String redisKey() default "";


    //限流的时间
    long time();

    //时间单位
    TimeUnit timeUnit()  default TimeUnit.SECONDS;

    //限流的次数
    long count();

    //接口的名称,描述接口的功能
    String name() default "limiter";




}
