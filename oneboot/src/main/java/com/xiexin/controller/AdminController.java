package com.xiexin.controller;

import com.xiexin.bean.Admin;
import com.xiexin.bean.AdminExample;
import com.xiexin.respcode.Result;
import com.xiexin.service.AdminService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;

   // 登录 Vue + ElementUI
    @RequestMapping("/login")
    public Map login(Admin admin, HttpSession session){
        Map codeMap=new HashMap();
        AdminExample example=new AdminExample();
        AdminExample.Criteria criteria = example.createCriteria();
        criteria.andAdminNameEqualTo(admin.getAdminName());
        criteria.andAdminPwdEqualTo(admin.getAdminPwd());


        List<Admin> admins = adminService.selectByExample(example);
        if(admins !=null && admins.size() >0){
            Admin dbadmins = admins.get(0);
            // 把这个账号信息  放入 session中
            session.setAttribute("dbadmins",dbadmins);
            System.out.println("dbadmins = " + dbadmins);
            codeMap.put("code",0);
            codeMap.put("msg","登录成功");
            codeMap.put("account",dbadmins.getAdminName());
            return codeMap;
        }else{
            codeMap.put("code",4001);
            codeMap.put("msg","账户或密码错误");
            return codeMap;
        }
    }

    // 登录 Vue + jquery
    @RequestMapping("/loginByShiro")
    public Result loginByShiro(@RequestBody Map map){
        // 登录交给 shiro的 securityManager 管理
        Subject subject = SecurityUtils.getSubject(); //subject 是根据  过滤器拿到的
        UsernamePasswordToken token = new UsernamePasswordToken((String)map.get("adminAccount"), (String)map.get("adminPwd"));

        // Ctrl+ Alt+ T  快捷键
        try {
            subject.login(token);  //ok
            if((Boolean) map.get("rememberMe")){
                System.out.println("执行记住我");
                token.setRememberMe(true);
            }

            return new Result();
        } catch (AuthenticationException e) {
            e.printStackTrace();
            return new Result(40001,"账户或者密码不正确");
        }
    }



    //业务层实现对明文密码的加密  注册 不能实现
//    @RequestMapping("/Reg")
//    public Integer toRegister(Admin admin){
//        /**
//         * 明文密码进行md5+salt+hash散列
//         * Md5Hash md5hash=new Md5Hash(password,""salt",hashIterations:8)
//         * */
//        String salt=getSalt(8);
//        //注册时，业务层方法需要修改，将随机盐保存到uer对象中，并一同保存到数据库
//        String encodedPassword =shiroPassword(admin.getAdminPwd(),salt);
//        System.out.println("业务层输出salt"+salt);
//        System.out.println("业务层输出password"+encodedPassword);
//        admin.setSalt(salt);
//        admin.setAdminPwd(encodedPassword);
//        return adminService.insertSelective(admin);
//
//    }

    // 注册   上课讲的 ： 业务层实现对明文密码的加密  注册
    @RequestMapping("/reg")
    public Result reg(@RequestBody Admin admin){
        String adminPwdMingWen = admin.getAdminPwd();  // 没加密的密码，  明文的

        //随机加盐 随机的几位字母作为salt
        //需要生成几位
        int n = 7;
        //最终生成的字符串
        String str = "";
        for (int i = 0; i < n; i++) {
            str = str + (char)(Math.random()*26+'a');
        }
        System.out.println(str);
        //给密码加盐 进行散列处理
        Md5Hash md5Pwd = new Md5Hash(adminPwdMingWen, str, 1024);
        System.out.println("md5Pwd = " + md5Pwd);
        admin.setAdminPwd(md5Pwd.toString());
        admin.setSalt(str); //加盐


        int i=adminService.insertSelective(admin);  // 传入的是 明文
        if(i == 1){
            return new Result();
        }else {
            return new Result(40001,"注册失败，用户名可能存在！！");
        }
    }



//    // 注册   普通的注册
//    @RequestMapping("/Reg")
//    public Map Reg(Admin admin){
//        Map map = new HashMap();
//            int i = adminService.insertSelective(admin);
//            if (i > 0) {
//                map.put("code", 200);
//                map.put("msg", "添加成功");
//                return map;
//            } else {
//                map.put("code", 400);
//                map.put("msg", "添加失败,检查网络再来一次");
//                return map;
//            }
//        }


    /***
     * 对用户的密码进行MD5加密
     * 做成工具类
     */
//    public static String shiroPassword(String adminPwd,String salt) {
//
//        // shiro 自带的工具类生成salt
//        //String salt = new SecureRandomNumberGenerator().nextBytes().toString();
//        // 加密次数
//        int times = 5;
//        // 算法名称
//        String algorithmName = "md5";
//
//        String encodedPassword = new SimpleHash(algorithmName,adminPwd,salt,times).toString();
//
//        // 返回加密后的密码
//        return encodedPassword;
//    }



    /**
     * 自定义生成随即盐salt的静态方法
     * */
//    public static String getSalt(int n) {
//        char[] chars = "ABCDEFGHIGKLMNOPQRSTUVWXYZabcdefghigklmnopqrstuvwxyz0123456789!@#$%^&*()".toCharArray();
//        StringBuilder stringBuilder = new StringBuilder();
//        //循环，每次取一个随机字符
//        for (int i = 0; i < n; i++) {
//            char achar = chars[new Random().nextInt(chars.length)];
//            stringBuilder.append(achar);
//        }
//        return stringBuilder.toString();
//
//    }

    // shiro 的退出 登录是 shiro管理， 退出也是 shiro 管理
    @RequestMapping("/logout")
    public Result logout(){
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
        return new Result();
    }

    }


