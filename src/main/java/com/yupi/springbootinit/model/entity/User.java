package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import io.swagger.models.auth.In;
import lombok.Data;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    //type = IdType.AUTO:就是自动生成的递增序列1、2、3、4、5.....
            //
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户剩余调用ai的次数
     */
    private Integer num;
    /**
     * 当前用户是否为会员
     */
    private Integer vip;

    /**
     * 是否删除
     */
    @TableLogic//开启逻辑删除
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}