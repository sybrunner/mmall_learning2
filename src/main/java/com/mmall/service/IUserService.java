package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;

/**
 * Created by apple on 17/7/29.
 */
//接口的声明
public interface IUserService {

    ServerResponse<User> login(String username, String password);//登陆

    ServerResponse<String> register(User user);//注册

    ServerResponse<String> checkValid(String str, String type);//校验用户

    ServerResponse selectQuestion(String username);//查找安全问题

    ServerResponse<String> checkAnswer(String username, String question, String answer);//检查答案

    ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken);//忘记密码的修改密码

    ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user);//登陆状态的修改密码

    ServerResponse<User> updateInformation(User user);//更新用户信息

    ServerResponse<User> getInformation(Integer userId);//获取用户信息

    ServerResponse checkAdminRole(User user);//校验用户是否是管理员



}
