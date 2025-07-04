package com.soultalk.mapper;

import com.soultalk.po.AgentPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMapper {
    void insert(AgentPO agent);

    AgentPO selectById(Long id);

    Integer countById(Long id);
}
