package com.limit.controller;


import com.limit.annotation.Limiter;
import com.limit.vo.ResponseVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/limiter")
public class TestController {

    //访问公共资源的测试，一段时间内固定次数
    @GetMapping("test1")
    @Limiter(redisKey = "'totalTime'",time = 120,count = 10,name = "公共资源")
    public ResponseVo test1() {
            return new ResponseVo().success().setMessage("test1访问成功");
    }

    //根据用户名限流
    @RequestMapping("test2")
    //SpEL表达式，接口的业务(接收验证码)+username
    @Limiter(redisKey = "'verificationCode_'+#username",time = 30,count = 1,name = "用户名私有资源")
    public ResponseVo test2(@RequestParam("username") String username){
        if("user".equals(username)){
            return new ResponseVo().success().setMessage("test2访问成功");
        }else {
            return new ResponseVo().fail().setMessage("非指定用户，访问失败");
        }
    }

    //根据用户id进行限流
    //SpEL表达式，接口的业务(访问)+userId
    @RequestMapping("test3")
    @Limiter(redisKey = "'visit_'+#userId",time = 60, count = 5,name = "用户id私有资源")
    public ResponseVo test3(@RequestParam("userId")int userId) {
        return new ResponseVo().success().setMessage("test3访问成功");
    }


    //访问公共资源的限制，总次数，当注解传入时间为-1时则为总次数的限制
    @GetMapping("test55")
    @Limiter(redisKey = "'total56'",time = -1,count = 10,name = "总资源")
    public ResponseVo test4() {
        return new ResponseVo().success().setMessage("test4访问成功");
    }

}
