package com.fastvue.module.project.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectMapper extends BaseMapper<ProjectEntity> {
    @Select("SELECT user_id FROM biz_project_member WHERE project_id = #{projectId}")
    List<Long> selectMemberIds(@Param("projectId") Long projectId);

    @Select("SELECT project_id FROM biz_project_member WHERE user_id = #{userId}")
    List<Long> selectProjectIdsForMember(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT project_id, user_id FROM biz_project_member WHERE project_id IN
            <foreach collection="projectIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY project_id, user_id
            </script>
            """)
    List<ProjectMemberRow> selectMemberRows(@Param("projectIds") List<Long> projectIds);

    @Select("SELECT count(*) > 0 FROM biz_project_member WHERE project_id = #{projectId} AND user_id = #{userId}")
    boolean isMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Insert("INSERT INTO biz_project_member(tenant_id, project_id, user_id, role) VALUES(#{tenantId},#{projectId},#{userId},#{role}) ON CONFLICT DO NOTHING")
    void addMember(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId,
                   @Param("userId") Long userId, @Param("role") String role);

    @Delete("DELETE FROM biz_project_member WHERE project_id = #{projectId} AND user_id = #{userId}")
    void removeMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    record ProjectMemberRow(Long projectId, Long userId) {}
}
