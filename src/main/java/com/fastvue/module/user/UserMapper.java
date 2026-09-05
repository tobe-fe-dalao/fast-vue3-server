package com.fastvue.module.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户数据访问。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * 查询用户拥有的角色编码。
     */
    @Select("""
            SELECT r.code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = false
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    /**
     * 查询用户通过角色间接拥有的权限字符串（去重）。
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId} AND p.deleted = false
            """)
    List<String> selectPermissionCodes(@Param("userId") Long userId);

    /**
     * 查询用户通过角色间接可访问的菜单 id（去重）。
     */
    @Select("""
            SELECT DISTINCT m.id
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.deleted = false
            """)
    List<Long> selectMenuIds(@Param("userId") Long userId);

    /**
     * 批量插入用户-角色关联。
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="roleIds" item="roleId" separator=",">
                (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 删除用户的所有角色关联。
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteUserRoles(@Param("userId") Long userId);
}
