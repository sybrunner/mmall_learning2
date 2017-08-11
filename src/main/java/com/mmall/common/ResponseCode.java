package com.mmall.common;

/**
 * Created by apple on 17/7/30.
 */
//枚举类
public enum   ResponseCode {
     //自定义函数 每个元组含有两个对象 例如 1: gode  SUCCESS:desc
     SUCCESS(0,"SUCCESS"),
     ERROR(1,"ERROR"),
     NEED_LOGIN(10,"NEED_LOGIN"),
     ILLEGAL_ARGUMENT(2,"ILLEGAL_ARGUMENT");

    private final int code; //final类型 一旦被初始化就不能更改  也不能变更引用(指针)
    private final String desc;

    //构造器
    ResponseCode(int code,String desc) {
        this.code = code;
        this.desc = desc;
    }
    //开放  get 方法
    public int getCode() {
        return code;
    }
    public String getDesc() {
        return  desc;
    }
}
