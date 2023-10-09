package com.lion.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lion.usercenter.common.ErrorCode;
import com.lion.usercenter.exception.BusinessException;
import com.lion.usercenter.mapper.UserMapper;
import com.lion.usercenter.model.domain.User;
import com.lion.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lion.usercenter.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author Lick
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2023-10-06 09:03:28
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private UserMapper userMapper;
    /**
     * 加密盐值
     */
    final String SALT = "lion";
    /**
     * 用户登录态
     */
//    public static final String USER_LOGIN_STATE = "userLoginState";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String vipCode) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            // done 抛出异常 封装异常
            throw new BusinessException(ErrorCode.NULL_ERROR, "用戶名密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短,至少4位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短,至少8位");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }
        // 验证特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不能包含特殊字符");
        }
        // 校验账户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号已存在");
        }
        if (vipCode != null && !vipCode.isEmpty()) {
            // 校验vipcode
            if (vipCode.length() > 9) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "vip码过长");
            }
            }
            QueryWrapper<User> queryWrapper_vipcode = new QueryWrapper<>();
            queryWrapper_vipcode.eq("vipCode", vipCode);
            long count_v = this.count(queryWrapper_vipcode);
            if (count_v > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "vip码已被使用");
            }
        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3.插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        // 默认用户名为账户
        user.setUsername(userAccount);
        // 默认头像
        user.setAvatarUrl("https://tse3-mm.cn.bing.net/th/id/OIP-C.3Pfd2kdG6S8b0JBZefXR6gAAAA?pid=ImgDet&rs=1");
        user.setUserPassword(encryptPassword);
        user.setVipCode(vipCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败, 恭喜你发现了一个未知bug");
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用戶名密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短,至少4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短,至少8位");
        }
        // 验证特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不能包含特殊字符");
        }
        // 校验账户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count == 0) {
            log.info("user login failed, user Account doesn't exist");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不存在");
        }
        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, user Account couldn't match password");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误");
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return safetyUser;
    }
    /**
     * 用户登出
     *
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }
    /**
     * 用户脱敏
     */
    @Override
    public User getSafetyUser(User originuser) {
        if (originuser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originuser.getId());
        safetyUser.setUserAccount(originuser.getUserAccount());
        safetyUser.setAvatarUrl(originuser.getAvatarUrl());
        safetyUser.setGender(originuser.getGender());
        safetyUser.setPhone(originuser.getPhone());
        safetyUser.setEmail(originuser.getEmail());
        safetyUser.setUserStatus(originuser.getUserStatus());
        safetyUser.setCreatetime(originuser.getCreatetime());
        safetyUser.setUpdatetime(originuser.getUpdatetime());
        safetyUser.setIsDelete(originuser.getIsDelete());
        safetyUser.setUsername(originuser.getUsername());
        safetyUser.setUserRole(originuser.getUserRole());
        safetyUser.setVipCode(originuser.getVipCode());
        return safetyUser;
    }

    /**
     * 用户信息修改
     * @param user
     * @return
     */
    @Override
    public int userUpdate(User user) {

        return 0;
    }
}




