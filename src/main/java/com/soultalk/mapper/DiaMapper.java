package com.soultalk.mapper;

import com.soultalk.po.DiaPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiaMapper {
    void insert(DiaPO dia);
}
