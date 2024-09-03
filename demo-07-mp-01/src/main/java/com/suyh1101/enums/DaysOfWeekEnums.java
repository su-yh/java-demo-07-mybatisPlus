package com.suyh1101.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @author suyh
 * @since 2024-09-03
 */
@Getter
public enum DaysOfWeekEnums {

    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    @EnumValue
    private final int dayOfWeek;

    DaysOfWeekEnums(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
}
