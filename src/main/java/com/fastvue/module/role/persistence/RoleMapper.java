package com.fastvue.module.role.persistence;

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

    @Select("""
            <script>
            SELECT rp.role_id, p.code FROM sys_role_permission rp
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE rp.role_id IN
            <foreach collection="roleIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            AND p.deleted = false
            ORDER BY rp.role_id, p.code
            </script>
            """)
    List<RolePermissionRow> selectPermissionRows(@Param("roleIds") List<Long> roleIds);

    @Select("""
            <script>
            SELECT role_id, menu_id FROM sys_role_menu WHERE role_id IN
            <foreach collection="roleIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY role_id, menu_id
            </script>
            """)
    List<RoleMenuRow> selectMenuRows(@Param("roleIds") List<Long> roleIds);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteRolePermissions(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteRoleMenus(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_permission (tenant_id, role_id, permission_id) VALUES
            <foreach collection="permissionIds" item="permissionId" separator=",">
                (#{tenantId}, #{roleId}, #{permissionId})
            </foreach>
            </script>
            """)
    void insertRolePermissions(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId,
                               @Param("permissionIds") List<Long> permissionIds);

    @Insert("""
            <script>
            INSERT INTO sys_role_menu (tenant_id, role_id, menu_id) VALUES
            <foreach collection="menuIds" item="menuId" separator=",">
                (#{tenantId}, #{roleId}, #{menuId})
            </foreach>
            </script>
            """)
    void insertRoleMenus(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId,
                         @Param("menuIds") List<Long> menuIds);

    record RolePermissionRow(Long roleId, String code) {}
    record RoleMenuRow(Long roleId, Long menuId) {}
}
