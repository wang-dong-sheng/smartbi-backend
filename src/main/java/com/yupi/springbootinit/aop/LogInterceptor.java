package com.yupi.springbootinit.aop;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.RedisConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求响应日志 AOP
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 **/
@Aspect
@Component
@Slf4j
public class LogInterceptor {
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HttpServletRequest  request;
    /**
     * 执行拦截
     */
    @Around("execution(* com.yupi.springbootinit.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 获取请求路径
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 生成请求唯一 id
        String requestId = UUID.randomUUID().toString();
        String url = httpServletRequest.getRequestURI();
        // 获取请求参数
        Object[] args = point.getArgs();
        String reqParam = "[" + StringUtils.join(args, ", ") + "]";
        // 输出请求日志
        log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url,
                httpServletRequest.getRemoteHost(), reqParam);
        // 执行原方法
        Object result = point.proceed();
        // 输出响应日志
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);
        return result;
    }


//    private void setNums(){
//        User loginUser = userService.getLoginUser(request);
//        // 通过权限校验，放行
//        //给用户授予访问次数
//        String key = RedisConstant.USER_SIGN__KEY + loginUser.getId();
//        //获取今天日期
//        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
////        获取当前是当月的第几天
//        int day = LocalDateTime.now().getDayOfMonth();
//        //查询当天是否记录已经登陆过
//        Boolean bit = stringRedisTemplate.opsForValue().getBit(key, (day - 1));
//        if (bit) {
////            已经访问,直接返回
//            return joinPoint.proceed();
//        }
//        //没访问过，设置为已经访问
//        stringRedisTemplate.opsForValue().setBit(key, day - 1, true);
//        //更新数据库，现在是软件测试阶段，直接进行次数覆盖即可
//        //查询当前是否为会员
//        User user = userService.getById(loginUser.getId());
//        if (user == null) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前用户不存在");
//        //是会员将次数设置为20次
//        Integer vip = user.getVip();
//        if (vip==0){
//            //非会员设置为3次
//            loginUser.setNum(3);
//        }else {
//            //会员20次
//            loginUser.setNum(20);
//        }
//        boolean b = userService.updateById(loginUser);
//        ThrowUtils.throwIf(b==false,ErrorCode.OPERATION_ERROR,"当前操作次数更新失败");
//        //放行
//    }
}

