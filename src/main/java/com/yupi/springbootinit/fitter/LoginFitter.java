package com.yupi.springbootinit.fitter;/**
 * @author Mr.Wang
 * @create 2023-11-09-9:24
 */

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.RedisConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *@ClassName LoginFitter
 *@Description TODO
 *@Author Mr.Wang
 *@Date 2023/11/9 9:24
 *@Version 1.0
 */

//@WebFilter(urlPatterns = {"/*"})
public class LoginFitter implements Filter {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserService userService;
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        //设置放行集合
        String[] going=new String[]{"login","register"};
        //为登录注册直接放行
        HttpServletRequest httpServletRequest= (HttpServletRequest) request;
        //获取url
        String requestURI = httpServletRequest.getRequestURI();
        for (String url: going) {
            if (requestURI.contains(url)){
                chain.doFilter(httpServletRequest,response);
                return;
            }
        }

        //查询是否有当前用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        ThrowUtils.throwIf(loginUser==null,ErrorCode.OPERATION_ERROR,"当前用户未登录");

        //放行
        chain.doFilter(httpServletRequest,response);

    }
}


