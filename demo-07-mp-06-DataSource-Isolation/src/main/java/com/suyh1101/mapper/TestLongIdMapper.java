package com.suyh1101.mapper;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.suyh1101.entity.TestLongIdEntity;
import com.suyh1101.mybatis.BaseMapperX;
import com.suyh1101.mybatis.safe.BaseMapperSafeX;
import org.apache.ibatis.annotations.Mapper;
import com.suyh1101.mybatis.LambdaQueryWrapperX;

/**
 * @author suyh
 * @since 2023-12-09
 */
@Mapper
public interface TestLongIdMapper extends BaseMapperSafeX<TestLongIdEntity> {
}
