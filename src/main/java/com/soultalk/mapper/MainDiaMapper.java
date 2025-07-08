package com.soultalk.mapper;

import com.soultalk.po.MainDiaPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MainDiaMapper {
    void insert(MainDiaPO dia);

    List<MainDiaPO> selectByUserId(Long userId);

    //获取user共有多少对话
    Integer countByUserId(Long userId);

    //删除用户所有对话
    void removeAllByUserId(Long userId);

    //删除用户指定索引对话，时间倒序
    void removeByIndex(Long userId, Integer index);
}
