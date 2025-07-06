package com.soultalk.mapper;

import com.github.pagehelper.Page;
import com.soultalk.po.UserEmotionRecordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserEmotionRecordMapper {
    
    Page<UserEmotionRecordPO> getEmotionPageList(Long userId);

    void insert(UserEmotionRecordPO userEmotionRecord);
}
