package com.suyh1101.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/**
 * @author suyh
 * @since 2023-12-09
 */
@TableName(value = "test_long_id", autoResultMap = true)
@Data
public class TestLongIdEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String nickName;

    private Integer age;

    @Version // 标注了 @Version 注解的字段被视为安全版本字段
    @TableField("version")
    private Integer safeVersion;
}
