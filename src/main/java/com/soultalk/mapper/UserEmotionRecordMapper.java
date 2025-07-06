package com.soultalk.mapper;

import com.github.pagehelper.Page;
import com.soultalk.po.UserEmotionRecordPO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserEmotionRecordMapper {

    Page<UserEmotionRecordPO> getEmotionPageList(Long userId);

    void insert(UserEmotionRecordPO userEmotionRecord);

    List<UserEmotionRecordPO> getEmotionChatList(Long userId, Integer items, LocalDateTime startTime, LocalDateTime endTime);

    void deleteBatch(List<Long> ids);
}
