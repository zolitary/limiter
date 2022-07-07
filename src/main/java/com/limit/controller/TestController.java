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

    @GetMapping("test1")
    @Limiter(time = 120,count = 10,name = "公共资源")
    public ResponseVo test1() {
            return new ResponseVo().success().setMessage("test1访问成功");
    }

    @RequestMapping("test2")
    @Limiter(redisKey = "#username",time = 10,count = 1)
    public ResponseVo test2(@RequestParam("username") String username){
        if("user".equals(username)){
            return new ResponseVo().success().setMessage("test2访问成功");
        }else {
            return new ResponseVo().fail().setMessage("非指定用户，访问失败");
        }
    }

    @RequestMapping("test3")
    @Limiter(redisKey = "#userId",time = 60, count = 5,name = "私有资源")
    public ResponseVo test3(@RequestParam("userId")int userId) {
        return new ResponseVo().success().setMessage("test3访问成功");
    }

}
