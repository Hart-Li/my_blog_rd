package com.study.blog.blog_admin.config.shiro;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.study.blog.blog_admin.config.shiro.jwt.JwtToken;
import com.study.blog.blog_admin.utils.JedisUtil;
import com.study.blog.blog_admin.utils.JwtUtil;
import com.study.blog.blog_core.constant.Constants;
import com.study.blog.blog_core.utils.StringUtil;
import com.study.blog.blog_model.pojo.Permission;
import com.study.blog.blog_model.pojo.Role;
import com.study.blog.blog_model.pojo.User;
import com.study.blog.blog_service.service.service.IPermissionService;
import com.study.blog.blog_service.service.service.IRolePermissionService;
import com.study.blog.blog_service.service.service.IRoleService;
import com.study.blog.blog_service.service.service.IUserRoleService;
import com.study.blog.blog_service.service.service.IUserService;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName UserRealm
 * @Description TODO
 * @Author Alex Li
 * @Date 2022/10/2 23:37
 * @Version 1.0
 */
@Service
@Slf4j
public class UserRealm extends AuthorizingRealm {

    private final IUserService userService;

    private final IUserRoleService userRoleService;

    private final IRoleService roleService;

    private final IRolePermissionService rolePermissionService;

    private final IPermissionService permissionService;

    @Autowired
    public UserRealm(IUserService userService, IUserRoleService userRoleService, IRoleService roleService,
                     IRolePermissionService rolePermissionService, IPermissionService permissionService) {
        this.userService           = userService;
        this.userRoleService       = userRoleService;
        this.roleService           = roleService;
        this.rolePermissionService = rolePermissionService;
        this.permissionService     = permissionService;
    }

    /**
     * ??????????????????????????????
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * ??????
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo simpleAuthenticationInfo = new SimpleAuthorizationInfo();
        String username = JwtUtil.getClaim(principals.toString(), Constants.USERNAME);
        // ????????????
        User user = this.userService.getByUsername(username);
        log.info("doGetAuthorizationInfo ??????????????????:{}", user);
        if (user != null) {
            // ????????????
            List<Long> roleIds = this.userRoleService.getRoleIdsByUserId(user.getId());
            log.info("doGetAuthorizationInfo ??????????????????????????????:{}", roleIds);
            List<Role> roles = this.roleService.listByIds(roleIds);
            log.info("doGetAuthorizationInfo ??????????????????????????????:{}", roles);
            if (CollectionUtils.isNotEmpty(roles)) {
                simpleAuthenticationInfo.setRoles(roles.stream().map(Role::getName).collect(Collectors.toSet()));
                // ????????????
                List<Long> permissionIds = this.rolePermissionService.getPermissionIdsByRoleIds(roleIds);
                List<Permission> permissions = this.permissionService.listByIds(permissionIds);
                simpleAuthenticationInfo
                    .setStringPermissions(permissions.stream().map(Permission::getName).collect(Collectors.toSet()));
            }
        }

        return simpleAuthenticationInfo;
    }

    /**
     * ??????
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String jwtToken = (String) token.getCredentials();
        String username = JwtUtil.getClaim(jwtToken, Constants.USERNAME);
        if (StringUtil.isBlank(username)) {
            throw new AuthenticationException("Token???????????????(The account in Token is empty.)");
        }
        // ????????????????????????
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new AuthenticationException("??????????????????(The account does not exist.)");
        }
        // ?????????????????? token ?????????????????? Redis ????????? RefreshToken ???????????? token ???????????????
        String redisKey = Constants.PREFIX_SHIRO_REFRESH_TOKEN + username;
        if (JwtUtil.verify(jwtToken) && JedisUtil.exists(redisKey)) {
            String currentTimeMillsRedis = String.valueOf(JedisUtil.getObject(redisKey));
            if (JwtUtil.getClaim(jwtToken, Constants.CURRENT_TIME_MILLIS).equals(currentTimeMillsRedis)) {
                return new SimpleAuthenticationInfo(jwtToken, jwtToken, "userRealm");
            }
        }
        throw new AuthenticationException("Token?????????(Token expired or incorrect.)");
    }
}
