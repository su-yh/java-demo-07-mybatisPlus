package com.suyh.mybatis.plus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author suyh
 * @since 2026-01-14
 */
public interface BaseMapperPlus<T> extends BaseMapper<T> {
    void createTableIfNotExists();
}
