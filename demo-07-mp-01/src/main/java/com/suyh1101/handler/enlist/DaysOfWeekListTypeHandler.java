package com.suyh1101.handler.enlist;

import com.suyh1101.enums.DaysOfWeekEnums;

/**
 * @author suyh
 * @since 2024-09-03
 */
public class DaysOfWeekListTypeHandler extends AbstractEnumListTypeHandler<DaysOfWeekEnums> {
    public DaysOfWeekListTypeHandler() {
        super(DaysOfWeekEnums.class);
    }
}
