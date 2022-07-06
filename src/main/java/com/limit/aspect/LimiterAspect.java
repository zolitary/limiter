package com.limit.aspect;

import com.limit.annotation.Limiter;
import com.limit.enums.LimitEnums;
import com.limit.exception.LimitException;
import com.limit.utils.HttpUtil;
import com.limit.utils.IPUtil;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Aspect;
import com.google.common.collect.ImmutableList;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;


@Slf4j
@Aspect
@Component
public class LimiterAspect extends MethodJoinPoint{

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Pointcut("@annotation(com.limit.annotation.Limiter)")
    public void pointcut(){

    }

    @Around("pointcut()")
    public Object aroundMethod(ProceedingJoinPoint point) throws Throwable{
        //获取HttpServletRequest
        HttpServletRequest request= HttpUtil.getHttpServletRequest();
        //使用继承的方法，通过反射获取当前连接点的方法
        Method method=resolveMethod(point);
        //获取自定义注解
        Limiter limitAnno=method.getAnnotation(Limiter.class);
        //判断限流的策略，是公共资源还是依据个人用户
        LimitEnums type=limitAnno.limitType();
        //要拼接的key
        String key;
        //接口名称,描述接口的功能
        String name= limitAnno.name();
        //获取用户ip
        String ip= IPUtil.getIpAddress(request);
        //获取限制时间和限制访问次数
        int time= limitAnno.time();
        int count= limitAnno.count();
        //根据类型对应是否需要拼接key
        switch (type){
            case IP : key= limitAnno.key()+":"+ip;
            break;
            case DEFAULT : key= limitAnno.key();
            break;
            //方法名转大写
            default: key= StringUtils.upperCase(method.getName());
        }

        //添加前缀将key转为List<String>,以便redisTemplate.execute方法使用。
        ImmutableList<String> keys = ImmutableList.of(StringUtils.join(limitAnno.pre() + "_", key));
//        String buildLua = buildLua();
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(buildLua());
        redisScript.setResultType(Long.class);
//        RedisScript<Number> redisScript=new DefaultRedisScript<>(buildLua,Number.class);
        Number totalCount=redisTemplate.execute(redisScript,keys,count,time);
        log.info("key为：{},用户IP：{},第{}次访问名字为【{}】的接口",keys,ip,totalCount,name);
        if(totalCount!=null&&totalCount.intValue()<=count){
            return point.proceed();
        }else {
            throw new LimitException("访问频率过高，请稍候再访问");
        }

    }

    //lua脚本
    private String buildLua() {
        return "local current" +
                "\n current = redis.call('get',KEYS[1])" +
                "\n if current and tonumber(current) > tonumber(ARGV[1]) then" +
                "\n return tonumber(current);" +
                "\n end" +
                "\n current = redis.call('incr',KEYS[1])" +
                "\n if tonumber(current) == 1 then" +
                "\n redis.call('expire',KEYS[1],ARGV[2])" +
                "\n end" +
                "\n return tonumber(current);";
    }


}
