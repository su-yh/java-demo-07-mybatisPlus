package com.suyh.mybatis.model.ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author suyh
 * @since 2026-01-14
 */
@Target(ElementType.FIELD) // 注解仅允许作用于：类的成员变量（普通属性）
@Retention(RetentionPolicy.RUNTIME) // 注解保留至运行时，支持反射获取（核心配置）
@Documented // 生成 JavaDoc 时，包含该注解的文档说明
@Inherited // 允许子类继承父类上的该注解（可选，根据业务需求决定是否开启）
public @interface TbColumn {

    /**
     * 字段名
     */
    String value();

    boolean enable() default true;

    /**
     * 字段数据类型（如 VARCHAR(32)、BIGINT、DECIMAL(18,2)）
     */
    String type();

    /**
     * 字段注释
     */
    String comment() default "";

    /**
     * 是否为主键
     */
    boolean primaryKey() default false;
}
