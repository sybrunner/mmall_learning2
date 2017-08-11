package com.mmall.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

/**
 * Created by apple
 */
//时间转化工具类
public class DateTimeUtil {

    //joda-time

    //str->Date
    //Date->str
    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";



    //转化方法 (str->Date)  formatStr:格式
    public static Date strToDate(String dateTimeStr,String formatStr){
        //声明
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(formatStr);//传入字符串格式
        DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);//datetime  进行接收
        return dateTime.toDate();
    }

    //转化方法 (Date->str)   formatStr:格式
    public static String dateToStr(Date date,String formatStr){
        if(date == null){
            return StringUtils.EMPTY; //返回空字符串
        }
        DateTime dateTime = new DateTime(date); //构造实例
        return dateTime.toString(formatStr); //转化
    }


    //重载
    public static Date strToDate(String dateTimeStr){
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(STANDARD_FORMAT);
        DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);
        return dateTime.toDate();
    }

    public static String dateToStr(Date date){
        if(date == null){
            return StringUtils.EMPTY;
        }
        DateTime dateTime = new DateTime(date);
        return dateTime.toString(STANDARD_FORMAT);
    }




//    public static void main(String[] args) {
//        System.out.println(DateTimeUtil.dateToStr(new Date(),"yyyy-MM-dd HH:mm:ss"));
//        System.out.println(DateTimeUtil.strToDate("2010-01-01 11:11:11","yyyy-MM-dd HH:mm:ss"));
//
//    }


}
