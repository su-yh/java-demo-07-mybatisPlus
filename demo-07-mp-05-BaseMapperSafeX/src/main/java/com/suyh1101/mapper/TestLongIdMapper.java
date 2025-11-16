package com.suyh1101.mapper;

import com.suyh1101.entity.TestLongIdEntity;
import com.suyh1101.mybatis.safe.BaseMapperSafeX;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

/**
 * @author suyh
 * @since 2023-12-09
 */
@Mapper
public interface TestLongIdMapper extends BaseMapperSafeX<TestLongIdEntity> {
    int insertEntities(Collection<TestLongIdEntity> entities);
}
