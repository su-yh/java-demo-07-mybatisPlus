package com.suyh.mybatis.test.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.suyh.mybatis.model.ann.TbColumn;

/**
 * @author suyh
 * @since 2026-01-14
 */
@TableName("temp_suyh")
public class TempEntity {
    @TbColumn(value = "id", type = "BIGINT", primaryKey = true)
    private Long id;

    @TbColumn(value = "name", type = "VARCHAR(32)", primaryKey = false)
    private String name;
}
