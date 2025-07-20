package com.ai.digitTeacher.mapper;

import com.ai.digitTeacher.entity.UserMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MemoryStoreMapper extends BaseMapper<UserMessage> {
    @Select("select messages from chat_memory where memory_id = #{memoryId}")
    String getMessages(@Param("memoryId") String memoryId);

    void insertOrUpdate(UserMessage userMessage);

}
