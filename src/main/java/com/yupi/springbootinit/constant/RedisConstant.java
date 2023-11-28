package com.yupi.springbootinit.constant;/**
 * @author Mr.Wang
 * @create 2023-11-01-11:24
 */

/**
 * reids使用到的常量
 *@InterfaceName RedisConstant
 *@Description TODO
 *@Author Mr.Wang
 *@Date 2023/11/1 11:24
 *@Version 1.0
 */
public interface RedisConstant {
    /**
     * 记录图表的分页查询的前缀
     */
    public final String CHAR_LIST_KEY="char:list:";

    /**
     * 用户签到的前缀
     */
    public final String USER_SIGN__KEY="user:sign:";
}
