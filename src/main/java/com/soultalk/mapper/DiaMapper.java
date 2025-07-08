package com.soultalk.mapper;

import com.soultalk.po.DiaPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DiaMapper {
    void insert(DiaPO dia);

    DiaPO selectDiaById(Long id);

    //start为起始索引(0),length取得最大长度
    List<DiaPO> selectRangeDia(Long id, Long start, Long length);

    Long countByUserId(Long id);

    void updateContent(Long id, String content);

    void updateLevel(Long id, Integer level);

    void deleteContent(Long id);

    void delete(Long id);

    void deleteByUserId(Long userId);
}
