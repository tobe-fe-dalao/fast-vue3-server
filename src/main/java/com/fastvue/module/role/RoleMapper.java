package com.fastvue.module.role;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色数据访问。
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {

    @Select("""
            SELECT p.code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId} AND p.deleted = false
            """)
    List<String> selectPermissionCodes(@Param("roleId") Long roleId);

    @Select("""
            SELECT menu_id
            FROM sys_role_menu
            WHERE role_id = #{roleId}
            """)
    List<Long> selectMenuIds(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteRolePermissions(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteRoleMenus(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_id) VALUES
            <foreach collection="permissionIds" item="permissionId" separator=",">
                (#{roleId}, #{permissionId})
            </foreach>
            </script>
            """)
    void insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    @Insert("""
            <script>
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES
            <foreach collection="menuIds" item="menuId" separator=",">
                (#{roleId}, #{menuId})
            </foreach>
            </script>
            """)
    void insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
