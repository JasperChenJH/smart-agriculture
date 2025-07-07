package com.soultalk.mapper;

import com.soultalk.po.MainDiaPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MainDiaMapper {
    void insert(MainDiaPO dia);

}
