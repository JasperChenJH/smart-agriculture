package com.soultalk.service.impl;

import com.soultalk.mapper.MainDiaMapper;
import com.soultalk.po.MainDiaPO;
import com.soultalk.service.MainAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Service
public class MainAgentServiceImpl implements MainAgentService {
    @Autowired
    private MainDiaMapper mainDiaMapper;

    @Override
    public Long createDia(Long userId) {
        MainDiaPO dia = new MainDiaPO();
        dia.setUserId(userId);
        dia.setContent(null);
        mainDiaMapper.insert(dia);
        return dia.getId();
    }

    @Override
    public MainDiaPO getDiaById(Long id) {
        return null;
    }

    @Override
    public SseEmitter streamQuestion(Long userId, String question) {
        return null;
    }

    @Override
    public Map<String, String> question(Long userId, String question) {
        return Map.of();
    }

    @Override
    public void removeContent(Long userId, Long diaId) throws Exception {

    }
}
