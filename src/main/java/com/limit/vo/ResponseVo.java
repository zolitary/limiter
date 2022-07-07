package com.limit.vo;

import org.springframework.http.HttpStatus;

import java.util.HashMap;

public class ResponseVo extends HashMap<String, Object> {

    @Override
    public ResponseVo put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public ResponseVo setData(Object data){
        this.put("data",data);
        return this;
    }

    public ResponseVo setStatus(HttpStatus httpStatus){
        this.put("status",httpStatus.value());
        return this;
    }

    public ResponseVo setMessage(String msg){
        this.put("message",msg);
        return this;
    }

    public ResponseVo success(){
        this.setStatus(HttpStatus.OK);
        return this;
    }

    public ResponseVo fail(){
        this.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return this;
    }

}
