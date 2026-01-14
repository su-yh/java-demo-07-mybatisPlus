package com.suyh.mybatis.test.service;

import com.suyh.mybatis.test.mapper.TempMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author suyh
 * @since 2026-01-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TempService {
    private final TempMapper tempMapper;

    @PostConstruct
    public void init() {
        // tempMapper.createTableIfNotExists();
    }
}
