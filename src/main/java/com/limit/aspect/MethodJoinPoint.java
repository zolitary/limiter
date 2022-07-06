package com.limit.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

//反射获取join point的方法
public class MethodJoinPoint {

    Method resolveMethod(ProceedingJoinPoint point) {
        //获取封装了署名信息的对象,在该对象中可以获取到目标方法名,所属类的Class等信息
        MethodSignature signature = (MethodSignature)point.getSignature();
        //getTarget获取被代理对象
        Class<?> targetClass = point.getTarget().getClass();

        Method method = getDeclaredMethod(targetClass, signature.getName(),
                signature.getMethod().getParameterTypes());
        if (method == null) {
            throw new IllegalStateException("无法解析目标方法: " + signature.getMethod().getName());
        }
        return method;
    }

    //反射获取有参方法
    private Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getDeclaredMethod(superClass, name, parameterTypes);
            }
        }
        return null;
    }
}
