package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.ICategoryService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by apple on 17/7/29.
 */
@Controller
@RequestMapping("/manage/category")//请求地址全部打到/manage/category 目录下 等价在每个 RequestMapping 方法后面写
public class CategoryManageController {


    @Autowired
    private IUserService iUserService;//注入 iUserService

    @Autowired
    private ICategoryService iCategoryService;//注入 ICategoryService


    //增加分类接口  (需要接收parentID,分类名,session) (GET)
    @RequestMapping("add_category.do")
    @ResponseBody  //返回自动通过SpringMVC的(json)插件 将返回值序列化为JSON

    //  没有传值默认 parentId = 0 (根节点)
    public ServerResponse addCategory(HttpSession session, String categoryName, @RequestParam(value = "parentId",defaultValue = "0") int parentId){

        //判断用户是否登陆(强制)
       User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录,请登录");
        }

        //校验一下是否是管理员
        if(iUserService.checkAdminRole(user).isSuccess()){
            //是管理员
            //增加我们处理分类的逻辑
            return iCategoryService.addCategory(categoryName,parentId);

        }else{
            return ServerResponse.createByErrorMessage("无权限操作,需要管理员权限");
        }
    }


    //更新category_name 的接口 (传入 session ,categoryID, name) //// TODO: 17/8/3    更新时间updatetime需要更新OK
    @RequestMapping("set_category_name.do")
    @ResponseBody
    public ServerResponse setCategoryName(HttpSession session, Integer categoryId, String categoryName){

        //强制登陆
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        //是否登陆
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录,请登录");
        }

        //判断用户是否有管理员权限
        if(iUserService.checkAdminRole(user).isSuccess()){
            //更新categoryName
            return iCategoryService.updateCategoryName(categoryId,categoryName);
        }else{
            return ServerResponse.createByErrorMessage("无权限操作,需要管理员权限");
        }
    }

    //查询子节点的category信息,并且不递归,保持平级 (入的节点做为被查询对象的父节点)
    @RequestMapping("get_category.do")
    @ResponseBody

    //使用  @RequestParam注解  没有传值默认 categoryId = 0 (根节点)
    public ServerResponse getChildrenParallelCategory(HttpSession session, @RequestParam(value = "categoryId" ,defaultValue = "0") Integer categoryId){

        //判断用户是否登陆
        User user = (User)session.getAttribute(Const.CURRENT_USER);

        //是否登陆
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录,请登录");
        }
        //管理员的判断
        if(iUserService.checkAdminRole(user).isSuccess()){
            //查询子节点的category信息,并且不递归,保持平级
            return iCategoryService.getChildrenParallelCategory(categoryId);
        }else{
            return ServerResponse.createByErrorMessage("无权限操作,需要管理员权限");
        }
    }


    //获取当前分类ID 并递归查询它的子节点的 ID
    @RequestMapping("get_deep_category.do")
    @ResponseBody
    public ServerResponse getCategoryAndDeepChildrenCategory(HttpSession session, @RequestParam(value = "categoryId" ,defaultValue = "0") Integer categoryId){

        //判断用户是否登陆
        User user = (User)session.getAttribute(Const.CURRENT_USER);

        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录,请登录");
        }

        //判断管理员
        if(iUserService.checkAdminRole(user).isSuccess()){
            //查询当前节点的id和递归子节点的id
//            0->10000->100000
            return iCategoryService.selectCategoryAndChildrenById(categoryId);

        }else{
            return ServerResponse.createByErrorMessage("无权限操作,需要管理员权限");
        }
    }








}
