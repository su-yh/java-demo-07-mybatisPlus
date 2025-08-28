package com.suyh1101.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.suyh1101.mybatis.safe.BaseMapperSafeX;
import com.suyh1101.mybatis.safe.BaseSafeUpdate;
import lombok.Data;

/**
 * @author suyh
 * @since 2023-12-09
 */
@TableName(value = "test_long_id", autoResultMap = true)
@Data
public class TestLongIdEntity implements BaseSafeUpdate {
    @TableId
    private Long id;

    private String nickName;

    private Integer age;

    @Version // 标注了 @Version 注解的字段被视为安全版本字段
    @TableField("version")
    private Integer version;

    @Override
    public Object getVersion() {
        return version;
    }
}
