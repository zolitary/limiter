package com.limit.aspect;

import com.limit.annotation.Limiter;
import com.limit.exception.LimitException;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
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

    //表达式的解析式
    private ExpressionParser parser = new SpelExpressionParser();

    @Pointcut("@annotation(limiter)")
    public void pointcut(Limiter limiter){

    }

    @Around("pointcut(limiter)")
    public Object aroundMethod(ProceedingJoinPoint point,Limiter limiter) throws Throwable{

        //获取连接点方法
        Method method = this.getMethod(point);
        String redisKey = method.toString();

        //如果redisKey不为空，则根据传入的参数来拼接
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

        //判断访问次数
        if (redisTemplate.opsForValue().get(redisKey) == null) {
            if(limiter.time()==-1){
                //不设置过期时间
                redisTemplate.opsForValue().set(redisKey, 1);
            }else {
                //设置过期时间
                redisTemplate.opsForValue().set(redisKey, 1, limiter.time(), timeUnit);
            }
            log.info("key为：{},第{}次访问名字为【{}】的接口",redisKey,1,name);
            return point.proceed();
        }else {
            int count=(int)redisTemplate.opsForValue().get(redisKey);
            if(count<limitAccount){
                //count+=1;
                //访问次数自增
                redisTemplate.opsForValue().increment(redisKey);
                log.info("key为：{},第{}次访问名字为【{}】的接口",redisKey,count+1,name);
                return point.proceed();
            }else {
                //访问次数超限
                Long expire = redisTemplate.getExpire(redisKey, timeUnit);
                if(expire==-1){
                    throw new LimitException("访问超限，该资源已不可访问");
                }else{
                    throw new LimitException("访问频繁，请等待[" + expire + "] " + timeUnit.name().toLowerCase() + "再重试");
                }
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

         //表达式的上下文对象
         EvaluationContext context = new StandardEvaluationContext();
        //将参数名与参数值对应起来
         for (int len = 0; len < params.length; len++) {
                 context.setVariable(params[len], args[len]);
             }
         return context;
     }

}
