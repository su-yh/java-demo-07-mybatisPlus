package com.suyh1101.mybatis.injector.methods;

import com.suyh1101.DemoMybatisPlusApplication;
import com.suyh1101.entity.TestLongIdEntity;
import com.suyh1101.mapper.TestLongIdMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


@ActiveProfiles("suyh")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = DemoMybatisPlusApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RequiredArgsConstructor
@Slf4j
public class InsertEntitiesTest {
    @Resource
    private TestLongIdMapper testLongIdMapper;

    @Test
    public void testInsert() {
        List<TestLongIdEntity> entities = new ArrayList<>();
        for (int i = 0; i < 2200; i++) {
            TestLongIdEntity insertEntity = new TestLongIdEntity();
            insertEntity.setNickName("nickName");
            insertEntity.setAge(10);
            insertEntity.setSafeVersion(1);
            entities.add(insertEntity);
        }

        testLongIdMapper.insertEntitiesBatch(entities, 1000);
    }
}

