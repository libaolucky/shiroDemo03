package com.xiexin.shiro;

import com.xiexin.bean.Admin;
import com.xiexin.bean.AdminExample;
import com.xiexin.service.AdminService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/*
*  自定义的 和 mybatis 数据库  结合的 realm
*
*   realm 中， 包含  认证（登录） 和 授权  两个部分
*    登录 为啥要继承 授权的 reaml  有 授权就一定是登录过了！
* */
public class MyBatisRealm extends AuthorizingRealm {
    @Autowired
   private AdminService adminService;
    // 授权
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        // 写授权！！！  查询3表 可得到 角色和权限。。。
        // 第一个， 拿到  account
        String principal = (String) principalCollection.getPrimaryPrincipal();
        Map map=new HashMap<>();
        map.put("adminAccount",principal);
        List<Map> maps = adminService.selectMore(map);
        // maps 包含了  角色名称， 权限名称
        Set<String> roleNames=new HashSet<>();
        List perms=new ArrayList();
        for (Map map1 : maps) {
            String roleName = (String) map1.get("roleName");
            String qxPerms = (String) map1.get("qxPerms");
            // 循环遍历到  roleNames  集合中
            roleNames.add(roleName);
            perms.add(qxPerms);
        }
            // 把角色和权限  给予 账户
            SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
            info.addRoles(roleNames);   // 给角色
            info.addStringPermissions(perms);  // 给权限
            return info;  // 触发 授权： 1. 界面UI触发，适用于 单体项目
                //2. java 方法注解触发， 适用于  前后端分离  3. 不常用的， 自已硬编码触发
                // 界面触发 要用到 aop jar 包的支持



        }


    // 认证（登录）
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        //AuthenticationToken 这个参数是啥？  其实就是  UsernamePasswordToken("账户"，“密码)
        String account= (String) authenticationToken.getPrincipal();
        // 拿到账户名后，  能拿到  数据库的 密码
        // 单表的查询
        AdminExample example=new AdminExample();
        AdminExample.Criteria criteria = example.createCriteria();
        criteria.andAdminAccountEqualTo(account);
        List<Admin> admins = adminService.selectByExample(example);
        Admin dbadmin=null;
        if(admins!=null && admins.size()>0){
             dbadmin=admins.get(0);
             // 在 controller 里面存的是账户和密码  在这里面 存储的是全部的信息
            Subject subject = SecurityUtils.getSubject();
            Session session = subject.getSession();
            session.setAttribute("dbadmin",dbadmin);

            // 获取 密码
            String pwd=dbadmin.getAdminPwd();
            System.out.println("pwd = " + pwd);
            String salt= dbadmin.getSalt();
            System.out.println("salt = " + salt);

            //进行 token 认证
            SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo(account, pwd, ByteSource.Util.bytes(salt),this.getName());
            System.out.println("ByteSource.Util.bytes(salt) = " + ByteSource.Util.bytes(salt));
                return simpleAuthenticationInfo;
        }
        return null;
    }
}
