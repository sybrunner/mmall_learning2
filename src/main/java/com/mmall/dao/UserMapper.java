package com.mmall.dao;

import com.mmall.pojo.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    int checkUsername(String username);

    int checkEmail(String email);

    //mybatis 传入多个参数用 @Param  (mybatis 注解的用法)
    User selectLogin(@Param("username") String username,@Param("password") String password);

    //查找问题
    String selectQuestionByUsername(String username);

    //校验用户问题的答案
    int checkAnswer(@Param("username")String username,@Param("question")String question,@Param("answer")String answer);

    //更新密码
    int updatePasswordByUsername(@Param("username")String username,@Param("passwordNew")String passwordNew);

    //验证密码
    int checkPassword(@Param("password")String password,@Param("userId")Integer userId);

    //检查邮箱
    int checkEmailByUserId(@Param(value="email")String email,@Param(value="userId")Integer userId);
}