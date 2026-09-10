package com.fastvue.module.organization.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentEntity> {
    @Select("SELECT user_id FROM org_department_member WHERE department_id = #{departmentId} ORDER BY joined_at")
    List<Long> selectMemberIds(@Param("departmentId") Long departmentId);

    @Select("SELECT count(*) FROM org_department_member WHERE department_id = #{departmentId}")
    long countMembers(@Param("departmentId") Long departmentId);

    @Delete("DELETE FROM org_department_member WHERE user_id = #{userId}")
    void removeUserMembership(@Param("userId") Long userId);

    @Delete("DELETE FROM org_department_member WHERE department_id = #{departmentId} AND user_id = #{userId}")
    void removeMember(@Param("departmentId") Long departmentId, @Param("userId") Long userId);

    @Insert("INSERT INTO org_department_member(tenant_id, department_id, user_id) VALUES (#{tenantId}, #{departmentId}, #{userId})")
    void addMember(@Param("tenantId") Long tenantId, @Param("departmentId") Long departmentId,
                   @Param("userId") Long userId);
}
