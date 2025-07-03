package com.soultalk.service.impl;

import com.soultalk.mapper.AgentMapper;
import com.soultalk.mapper.DiaMapper;
import com.soultalk.po.DiaPO;
import com.soultalk.service.DiaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DiaServiceImpl implements DiaService {
    @Autowired
    private DiaMapper diaMapper;
    @Autowired
    private AgentMapper agentMapper;

    @Override
    public long createDia(Long userId, Long agentId) {
        DiaPO dia = new DiaPO();
        dia.setUserId(userId);
        //检查绑定agent
        if (agentId != null && agentMapper.countById(agentId) > 0) {
            dia.setAgentId(agentId);
            dia.setIsAgent(1);
        } else {
            dia.setIsAgent(0);
        }
        dia.setContent(null);
        dia.setUpdateTime(System.currentTimeMillis());
        diaMapper.insert(dia);
        return dia.getId();
    }

    @Override
    public DiaPO getDiaById(Long id) {
        return diaMapper.selectDiaById(id);
    }

    @Override
    public long countDiaByUserId(Long userId) {
        return diaMapper.countByUserId(userId);
    }

    @Override
    public List<DiaPO> getRangeDia(Long userId, Long start, Long end) {
        //交换start和end
        if (start > end) {
            start += end;
            end = start - end;
            start = start - end;
        }

        //调整最大id
        Long maxId = diaMapper.countByUserId(userId);
        if (end > maxId) {
            end = maxId;
        }

        Long length = end - start;
        return diaMapper.selectRangeDia(userId, start, length);
    }

}
