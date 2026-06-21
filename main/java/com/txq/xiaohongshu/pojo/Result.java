package com.txq.xiaohongshu.pojo;

import lombok.Data;

@Data
public class Result {
   private int code;
   private String msg;
   private Object data;
   public static Result error(){
       Result result = new Result();
       result.setCode(0);
       result.setMsg("出错了，请联系后台管理人员");
       return result;
   }
    public static Result error(String msg){
        Result result = new Result();
        result.setCode(0);
        result.setMsg(msg);
        return result;
    }
    public static Result sucess(Object data){
       Result result = new Result();
       result.setCode(1);
       result.setMsg("success");
       result.setData(data);
       return result;
    } public static Result sucess(){
        Result result = new Result();
        result.setCode(1);
        result.setMsg("success");
        return result;
    }
}