package com.limit.aspect;

import com.limit.annotation.Limiter;
import com.limit.exception.LimitException;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Aspect;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;


@Slf4j
@Aspect
@Component
public class LimiterAspect{

    @Resource
    RedisTemplate redisTemplate;

    private DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    private ExpressionParser parser = new SpelExpressionParser();

    @Pointcut("@annotation(limiter)")
    public void pointcut(Limiter limiter){

    }

    @Around("pointcut(limiter)")
    public Object aroundMethod(ProceedingJoinPoint point,Limiter limiter) throws Throwable{

        Method method = this.getMethod(point);
        String redisKey = method.toString();

        if (StringUtils.isNotBlank(limiter.redisKey())) {

            //获取方法的参数值
            Object[] args = point.getArgs();
            EvaluationContext context = this.bindParam(method, args);

            //根据SpEL表达式获取值
            Expression expression = parser.parseExpression(limiter.redisKey());
            Object theValue = expression.getValue(context);
            redisKey += "_" + theValue.toString();
        }else {
            redisKey+="_common";
        }
        TimeUnit timeUnit = limiter.timeUnit();
        int limitAccount=limiter.count();
        String name= limiter.name();

        if (redisTemplate.opsForValue().get(redisKey) == null) {
            redisTemplate.opsForValue().set(redisKey, 1, limiter.time(), timeUnit);
            log.info("key为：{},第{}次访问名字为【{}】的接口",redisKey,1,name);
            return point.proceed();
        }else {
            int count=(int)redisTemplate.opsForValue().get(redisKey);
            if(count<limitAccount){
                //count+=1;
                redisTemplate.opsForValue().increment(redisKey);
                log.info("key为：{},第{}次访问名字为【{}】的接口",redisKey,count+1,name);
                return point.proceed();
            }else {
                Long expire = redisTemplate.getExpire(redisKey, timeUnit);
                throw new LimitException("访问频繁，请等待[" + expire + "] " + timeUnit.name().toLowerCase() + "再重试");
            }

        }

    }

    //获取当前执行的方法
    private Method getMethod(ProceedingJoinPoint point) throws NoSuchMethodException {
         MethodSignature methodSignature = (MethodSignature) point.getSignature();
         Method method = methodSignature.getMethod();
         Method targetMethod = point.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());
         return targetMethod;
     }

     //将方法的参数名和参数值绑定，params对应参数名，args对应参数值
    private EvaluationContext bindParam(Method method, Object[] args) {
         //获取方法的参数名
         String[] params = discoverer.getParameterNames(method);

         //将参数名与参数值对应起来
         EvaluationContext context = new StandardEvaluationContext();
         for (int len = 0; len < params.length; len++) {
                 context.setVariable(params[len], args[len]);
             }
         return context;
     }

}
