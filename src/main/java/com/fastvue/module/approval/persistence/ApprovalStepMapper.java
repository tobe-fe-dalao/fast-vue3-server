package com.fastvue.module.approval.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ApprovalStepMapper extends BaseMapper<ApprovalStepEntity> {
    @Select(
            "SELECT approval_request_id FROM wf_approval_step WHERE approver_id = #{userId} AND status = 'PENDING'")
    List<Long> selectPendingRequestIds(@Param("userId") Long userId);
}
