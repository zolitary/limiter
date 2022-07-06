package com.limit.utils;

import javax.servlet.http.HttpServletRequest;


public class IPUtil {

    protected IPUtil(){
    }

    //获取ip地址
    public static String getIpAddress(HttpServletRequest request) {
        // 获取客户端ip地址
        String ipStr = request.getHeader("x-forwarded-for");
        if (ipStr == null || ipStr.length() == 0 || "unknown".equalsIgnoreCase(ipStr)) {
            ipStr = request.getHeader("Proxy-Client-IP");
        }
        if (ipStr == null || ipStr.length() == 0 || "unknown".equalsIgnoreCase(ipStr)) {
            ipStr = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipStr == null || ipStr.length() == 0 || "unknown".equalsIgnoreCase(ipStr)) {
            ipStr = request.getRemoteAddr();
        }

        // 多个路由时，取第一个非unknown的ip
        final String[] arr = ipStr.split(",");
        for (final String str : arr) {
            if (!"unknown".equalsIgnoreCase(str)) {
                ipStr = str;
                break;
            }
        }

        //目的是将localhost访问对应的ip 0:0:0:0:0:0:0:1 转成 127.0.0.1。
        return ipStr.equals("0:0:0:0:0:0:0:1") ? "127.0.0.1" : ipStr;
    }

}
