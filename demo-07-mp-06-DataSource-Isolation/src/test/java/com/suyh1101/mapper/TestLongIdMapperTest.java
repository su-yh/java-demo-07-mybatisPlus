package com.suyh1101.mapper;

import com.suyh1101.DemoMybatisPlusApplication;
import com.suyh1101.entity.TestLongIdEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AssertionsKt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
  * @author suyh
  * @since 2025-08-28
  */
@ActiveProfiles("suyh")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = DemoMybatisPlusApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RequiredArgsConstructor
@Slf4j
public class TestLongIdMapperTest {
    @Resource
    private TestLongIdMapper testLongIdMapper;

}