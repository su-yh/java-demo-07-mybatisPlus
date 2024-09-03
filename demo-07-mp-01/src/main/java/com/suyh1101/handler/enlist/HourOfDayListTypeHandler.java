package com.suyh1101.handler.enlist;

import com.suyh1101.enums.HourOfDayEnums;

/**
 * @author suyh
 * @since 2024-09-03
 */
public class HourOfDayListTypeHandler extends AbstractEnumListTypeHandler<HourOfDayEnums> {
    public HourOfDayListTypeHandler() {
        super(HourOfDayEnums.class);
    }
}
