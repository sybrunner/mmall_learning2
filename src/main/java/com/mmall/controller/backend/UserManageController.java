package com.mmall.controller.backend;

import com.mmall.common.Const;
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

@Controller  //controller注解
@RequestMapping("/manage/user")//请求地址全部打到/manage/user/ 目录下 等价在每个 RequestMapping 方法后面写
public class UserManageController {

    @Autowired   //注入iUserService
    private IUserService iUserService;


    //后台管理员登陆
    @RequestMapping(value="login.do",method = RequestMethod.POST)
    @ResponseBody  //返回自动通过SpringMVC的(json)插件 将返回值序列化为JSON
    public ServerResponse<User> login(String username, String password, HttpSession session){

        //登陆
        ServerResponse<User> response = iUserService.login(username,password);
        if(response.isSuccess()){
            User user = response.getData();

            //权限判断 说明登录的是管理员
            if(user.getRole() == Const.Role.ROLE_ADMIN){

                //把这个用户放入session
                session.setAttribute(Const.CURRENT_USER,user);
                return response;
            }else{
                return ServerResponse.createByErrorMessage("不是管理员,无法登录");
            }
        }
        return response;
    }

}
