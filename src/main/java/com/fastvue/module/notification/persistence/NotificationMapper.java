package com.fastvue.module.notification.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {
    @Update("UPDATE sys_notification SET read = true, updated_at = now() WHERE receiver_id = #{receiverId} AND read = false")
    void markAllRead(@Param("receiverId") Long receiverId);
}
