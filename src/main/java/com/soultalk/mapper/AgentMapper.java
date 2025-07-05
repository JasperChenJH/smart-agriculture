package com.soultalk.mapper;

import com.soultalk.po.AgentPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentMapper {
    void insert(AgentPO agent);

    AgentPO selectById(Long id);

    List<AgentPO> selectByUserId(Long userId);

    //模糊匹配名字查找智能体
    List<AgentPO> selectLikeByName(String name);

    Integer countById(Long id);

    void update(AgentPO agent);

    //查询自己创建的智能体
    List<AgentPO> selectMyAgent(Long userId);
}
