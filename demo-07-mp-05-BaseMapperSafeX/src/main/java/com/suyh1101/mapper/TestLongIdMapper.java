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
    // 批量插入要注意的是自增主键ID 不会回写。所以如果需要自增主键ID 的话，就不能使用该方法进行批量插入。
    int insertEntities(Collection<TestLongIdEntity> entities);
}
