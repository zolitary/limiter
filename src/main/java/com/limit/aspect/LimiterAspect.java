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
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Aspect;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;


@Slf4j
@Aspect
@Component
public class LimiterAspect{

    @Resource
    RedisTemplate redisTemplate;

    private DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    //表达式的解析器
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

        //限流的时间单位
        TimeUnit timeUnit = limiter.timeUnit();
        //限制访问次数
        int limitAccount=limiter.count();
        String name= limiter.name();

        //使用redis原子整型RedisAtomicInteger
        RedisAtomicInteger atomicCount=new RedisAtomicInteger(redisKey,redisTemplate.getConnectionFactory());
        //获取当前访问次数
        int count=atomicCount.getAndIncrement();
        //如果是首次访问
        if(count==0){
            //需要设置过期时间
            if(limiter.time()!=-1){
                atomicCount.expire(limiter.time(),timeUnit);
                log.info("限流配置：key为{}，{} {}内允许访问{}次",redisKey,limiter.time(),timeUnit,limitAccount);
            }else {
                //无需设置过期时间
                log.info("限流配置：key为{}，总共只能允许访问{}次",redisKey,limiter.time(),timeUnit,limitAccount);
            }
            //输出访问信息
            log.info("key为：{},第{}次访问名字为【{}】的接口",redisKey,1,name);
            return point.proceed();
        }else {
            //访问次数未超限，放行并打印信息
            if(count<limitAccount){
                //输出访问的信息
                log.info("key为：{},第{}次访问名字为【{}】的接口",redisKey,count+1,name);
                return point.proceed();
            }else {
                //访问次数超限，抛出异常
                Long expire = redisTemplate.getExpire(redisKey, timeUnit);
                if(expire==-1){
                    throw new LimitException("访问超限，该资源已不可访问");
                }else{
                    throw new LimitException("访问频繁，请等待[" + expire + "] " + timeUnit.name().toLowerCase() + "再重试");
                }
            }
        }

        /**
         * 存在并发问题
         */
        //Object nowCount = redisTemplate.opsForValue().get(redisKey);
        //判断访问次数
//        if (nowCount==0) {
//            if(limiter.time()==-1){
//                //不设置过期时间
//                redisTemplate.opsForValue().set(redisKey, 1);
//            }else {
//                //设置过期时间
//                redisTemplate.opsForValue().set(redisKey, 1, limiter.time(), timeUnit);
//            }
//            log.info("key为：{},第{}次访问名字为【{}】的接口",redisKey,1,name);
//            return point.proceed();
//        }else {
//            int count=(int)nowCount;
//            if(count<limitAccount){
//                //访问次数自增
//                redisTemplate.opsForValue().increment(redisKey);
//                //输出访问的信息
//                log.info("key为：{},第{}次访问名字为【{}】的接口",redisKey,count+1,name);
//                return point.proceed();
//            }else {
//                //访问次数超限
//                Long expire = redisTemplate.getExpire(redisKey, timeUnit);
//                if(expire==-1){
//                    throw new LimitException("访问超限，该资源已不可访问");
//                }else{
//                    throw new LimitException("访问频繁，请等待[" + expire + "] " + timeUnit.name().toLowerCase() + "再重试");
//                }
//            }
//        }

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
