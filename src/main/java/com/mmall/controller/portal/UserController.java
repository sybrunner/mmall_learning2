package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by apple on 17/7/29.
 */
@Controller
@RequestMapping("/user/")//请求地址全部打到user/ 目录下 等价在每个 RequestMapping 方法后面写
public class UserController {

    @Autowired//自动注入  注解 ,方便使用
    private IUserService iUserService;//注入UserService

    /**
     * 用户登陆
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do",method = RequestMethod.POST)
    @ResponseBody //返回自动通过SpringMVC的(json)插件 将返回值序列化为JSON
    public ServerResponse<User> login(String username, String password, HttpSession session) {
        //service -->mybats--dao 完成..

        ServerResponse<User> response = iUserService.login(username, password);
        //状态码为0 条件成立 返回true
        if (response.isSuccess()) {
            session.setAttribute(Const.CURRENT_USER,response.getData());//当前用户数据保存到session 作用域中(全局)
        }
        return response;
    }
    //登出接口
    @RequestMapping(value = "logout.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session) {

        session.removeAttribute(Const.CURRENT_USER); //移除 session对象
        return ServerResponse.createBySuccess();//返回状态码 0  成功
    }
    //注册接口
    @RequestMapping(value = "register.do",method = RequestMethod.POST)
    @ResponseBody
    //(User user)==(username,username,username,username)
    public ServerResponse<String> register(User user) {
        return iUserService.register(user);
    }

    //当用户输入完成名字 实时调用校验接口
    @RequestMapping(value = "check_valid.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String str,String type) {
       return iUserService.checkValid(str, type);
   }

    //获取登陆用户信息的接口
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo (HttpSession session){
        User user =  (User)session.getAttribute(Const.CURRENT_USER);//从session获取user   强转(User)
        if (user != null) {
            return ServerResponse.createBySuccess(user);//成功 并放入数据
        }
        //ServerResponse可以理解为响应对象    (返回下面字段 标示状态码为1)
        return ServerResponse.createByErrorMessage("用户未登陆,无法获取当前用户信息");
    }

    //忘记密码  获取问题
    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username){
        return iUserService.selectQuestion(username);
    }

    //校验用户问题答案是否正确
    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username,String question,String answer){
        return iUserService.checkAnswer(username,question,answer);
    }

    //忘记密码的重置密码
    @RequestMapping(value = "forget_reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken){
        return iUserService.forgetResetPassword(username,passwordNew,forgetToken);
    }

    //登陆状态的重置密码
    @RequestMapping(value = "reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    //判断登陆状态放入session
    public ServerResponse<String> resetPassword(HttpSession session,String passwordOld,String passwordNew){

        //获取用户并判断是否登陆
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        return iUserService.resetPassword(passwordOld,passwordNew,user);
    }

    //更新个人用户信息
    @RequestMapping(value = "update_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> update_information(HttpSession session, User user){

        //获取登陆用户的user id
        //防止id被变化 阻止横向越权
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);

        //判断登陆
        if(currentUser == null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }

        //当前登陆的ID传如 user对象
        user.setId(currentUser.getId());
        //因为updateInformation  不更新 username , 得set上
        user.setUsername(currentUser.getUsername());

        //传入用户进行更新,它的ID,和username  都是从登陆用户获取的
        ServerResponse<User> response = iUserService.updateInformation(user);
        if(response.isSuccess()){

            //更新成功则response获得对象 并更新session
            response.getData().setUsername(currentUser.getUsername());
            session.setAttribute(Const.CURRENT_USER,response.getData());
        }
        return response;
    }

    //获取用户详细信息  如果用户没有登陆 要进行强制登陆
    @RequestMapping(value = "get_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> get_information(HttpSession session){

        //获取登陆用户的对象(从session)
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            //传10过去前端进行强制登陆
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录,需要强制登录status=10");
        }
        return iUserService.getInformation(currentUser.getId());//返回 前台能看到个人详情信息
    }
}
