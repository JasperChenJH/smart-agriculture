package com.soultalk.service;

import org.springframework.web.multipart.MultipartFile;

public interface BaseService {
    String saveFileToOSS(String fileName, MultipartFile file);
}
