package com.lion.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private String gender;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 账号状态
     */
    private Integer userStatus;

    /**
     * 时间
     */
    private Date createtime;

    /**
     * 更新时间
     */
    private Date updatetime;

    /**
     * 删除是否
     */
    private Integer isDelete;

    /**
     * 用户名
     */
    private String username;

    /**
     * admin-管理员  user-普通用户
     */
    private String userrole;

    /**
     * "用户vip编号"
     */
    private String vipCode;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}