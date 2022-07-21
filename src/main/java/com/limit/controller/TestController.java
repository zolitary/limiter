package com.limit.controller;


import com.limit.annotation.Limiter;
import com.limit.domain.User;
import com.limit.vo.ResponseVo;
import org.springframework.web.bind.annotation.*;

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
            return new ResponseVo().success().setMessage("test2访问成功");


    }

    //根据用户id进行限流
    //SpEL表达式，接口的业务(访问)+userId
    //user.id
    @RequestMapping("test3")
    @Limiter(redisKey = "'visit_'+#userId",time = 60, count = 5,name = "用户id私有资源")
    public ResponseVo test3(@RequestParam("userId")int userId) {
        return new ResponseVo().success().setMessage("test3访问成功");
    }


    //访问公共资源的限制，总次数，当注解传入时间为-1时则为总次数的限制
    @GetMapping("test4")
    @Limiter(redisKey = "'totalCount'",time = -1,count = 10,name = "总资源")
    public ResponseVo test4() {
        return new ResponseVo().success().setMessage("test4访问成功");
    }

    //传入参数为实体类，测试
    @RequestMapping(value = "test5",method = RequestMethod.POST)
    @Limiter(redisKey = "'newTest_'+#user.id",time = 60, count = 5,name = "用户id私有资源")
    public ResponseVo test5(@RequestBody User user) {
        return new ResponseVo().success().setMessage("test5访问成功");
    }



}
