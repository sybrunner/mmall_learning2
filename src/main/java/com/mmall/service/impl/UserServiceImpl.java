package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by apple on 17/7/29.
 */
//接口的实现
@Service("iUserService")//声明服务  注入到controller供其调用
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;

    @Override

    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");//首先检查用户名是否存在
        }
        //将登陆密码设置为MD5加密
        String md5Password = MD5Util.MD5EncodeUtf8(password);

        User user = userMapper.selectLogin(username, md5Password);//User 类型是pojo里面的 密码对照加密后的密码表
        //数据库中查找对象 没有则密码不正确
        if (user == null) {
            return ServerResponse.createByErrorMessage("密码错误");
        }
        //处理返回值密码  使其为空 (因为密码也会被返回出来)
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登陆成功", user);//返回状态码 0, msg 和 user
    }

    public ServerResponse<String> register(User user) {

//        int resultCount = userMapper.checkUsername(user.getUsername());
//        if (resultCount > 0) {
//            return ServerResponse.createByErrorMessage("用户名已存在");
//        }
        //检测是否存在用户名   传入值和 user name
        ServerResponse validResponse = this.checkValid(user.getUsername(), Const.USERNAME);
        //不成功 返回
        if (!validResponse.isSuccess()) {
            return validResponse;
        }
//        resultCount = userMapper.checkEmail(user.getEmail());
//        if (resultCount > 0) {
//            return ServerResponse.createByErrorMessage("email已存在");
//        }
        ////检测是否存在email   传入值和 user name
        validResponse = this.checkValid(user.getEmail(), Const.EMAIL);
        if (!validResponse.isSuccess()) {
            return validResponse;
        }


        user.setRole(Const.Role.ROLE_CUSTOMER);//默认设置普通用户

        //md5 加密
        //传入秘密到唯一的共有方法进行加密后返回
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        int resultCount = userMapper.insert(user);//将对象插入用户表
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    //校验接口的实现
    //str 是value值   type 是 email或者是username
    //用户存在 返回错误对象  不存在 返回成功对象
    //首先校验用户是否存在  存在返回status =1 (不能注册) 反之 status = 0 (可以注册)
    public ServerResponse<String> checkValid(String str, String type){
        if (StringUtils.isNotBlank(type)) {
            //type不是空 开始校验  空格判断false  而NotEmpty对空格的判断是ture
            //判断传的是 用户名 还是 email
            if (Const.USERNAME.equals(type)) {
                int resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if (Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("email已存在");
                }
            }
        }else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }

    //忘记密码 搜索问题
    public ServerResponse selectQuestion(String username){

        //首先校验用户是否存在  存在返回status =1  反之 status = 0
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);

        //检查对象状态信息   对象返回的status = 1 (错误)  则表有用户 validResponse
        //返回的是 status = 0  则表没有用户(对象)
        if(validResponse.isSuccess()){
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        //调用dao层查询用户设置的问题
        String question = userMapper.selectQuestionByUsername(username);

        //问题非空则返回 (空格也算空)
        if(StringUtils.isNotBlank(question)){
            return ServerResponse.createBySuccess(question);
        }
        //反之是空的 返回错误响应对象
        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    //校验问题答案
    public ServerResponse<String> checkAnswer(String username, String question, String answer) {
        int resultCount = userMapper.checkAnswer(username,question,answer);

        //resultCount > 0 说明问题以及答案是这个用户的,并且是正确的
        if (resultCount > 0) {

            //用UUID方法 生成token 重复率微乎其微
            String forgetToken = UUID.randomUUID().toString();

            //把forgetToken 放入本地缓存
            // TokenCache.TOKEN_PREFIX = "token_" :(名字前缀,便于区分)
            TokenCache.setKey(TokenCache.TOKEN_PREFIX+username,forgetToken);

            return ServerResponse.createBySuccess(forgetToken);


        }
        return ServerResponse.createByErrorMessage("问题的答案错误");
    }

    //重置密码
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken){

        //校验token参数
        if(StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("参数错误,token需要传递");
        }


        //校验用户名
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess()){
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }


        //获取token 并校验
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
        if(StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("token无效或者过期");
        }


        //如果传入token == 生成的token
        if(StringUtils.equals(forgetToken,token)){

            //将新密码进行加密
            String md5Password  = MD5Util.MD5EncodeUtf8(passwordNew);

            //更新
            int rowCount = userMapper.updatePasswordByUsername(username,md5Password);

            //生效行数大于0
            if(rowCount > 0){
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
        }

        else{
            return ServerResponse.createByErrorMessage("token错误,请重新获取重置密码的token");
        }
        return ServerResponse.createByErrorMessage("修改密码失败");
    }

    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user){
        //防止横向越权,要校验一下这个用户的旧密码,一定要指定是这个用户.因为我们会查询一个count(1),如果不指定id,那么结果就是true啦count>0;
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("旧密码错误");
        }

        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));

        //选择性更新 哪个不为空就更新哪个
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0){
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    public ServerResponse<User> updateInformation(User user){
        //username是不能被更新的
        //email也要进行一个校验,出了当前用户 还有人使用这个email吗?
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if(resultCount > 0){
            return ServerResponse.createByErrorMessage("email已存在,请更换email再尝试更新");
        }

        //创建一个更新对象
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());


        //不等于空的时候就更新
        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0){
            return ServerResponse.createBySuccess("更新个人信息成功",updateUser);
        }
        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }


    //更新用户信息
    public ServerResponse<User> getInformation(Integer userId){

        //获取userID
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null){
            return ServerResponse.createByErrorMessage("找不到当前用户");
        }

        //处理返回值密码  使其为空 (因为密码也会被返回出来 不能返回给前台)
        user.setPassword(StringUtils.EMPTY);

        return ServerResponse.createBySuccess(user);

    }


    //backend

    /**
     * 校验是否是管理员
     * @param user
     * @return
     */
    public ServerResponse checkAdminRole(User user){

        // 0 普通用户  , 1 管理员用户
        if(user != null && user.getRole().intValue() == Const.Role.ROLE_ADMIN){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

}
