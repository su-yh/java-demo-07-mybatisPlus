package com.suyh1101.generator;

/**
 * @author suyh
 * @since 2025-09-20
 */
public enum UuidModeEnums {
    // 正常顺序
    ORDERED_NORMAL,
    // 正常乱序
    UNORDERED_NORMAL,
    // 乱序再额外增加3 随机字节
    UNORDERED_PLUS,
    ;
}
