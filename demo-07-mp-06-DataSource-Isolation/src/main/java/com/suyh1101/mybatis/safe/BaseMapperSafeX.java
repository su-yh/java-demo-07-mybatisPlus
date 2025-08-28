package com.suyh1101.mybatis.safe;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.suyh1101.mybatis.BaseMapperX;
import org.apache.ibatis.annotations.Param;

/**
 * @author suyh
 * @since 2025-08-28
 */
public interface BaseMapperSafeX<T extends BaseSafeUpdate> extends BaseMapperX<T> {

    /**
     * 核心还是调用 {@link #updateById(Object)} 这里只是简单的封装了一下，并抛出了异常。
     * 如果不期望抛异常，可以直接调用 updateById 方法。
     * 这里检查了version 的null 值情况，以及结果判断。
     */
    default void updateByIdForSafeVersion(@Param(Constants.ENTITY) T entity) {
        if (entity.getVersion() == null) {
            // Safe update version cannot be null. entity: {0}
            // TODO: suyh - 这里换成业务上面的异常
            throw new RuntimeException();
        }

        int res = updateById(entity);
        if (res != 1) {
            // 这里不考虑ID 不存在的情况。
            // Update failed, please retry.
            // TODO: suyh - 这里换成业务上面的异常
            throw new RuntimeException();
        }
    }

    // suyh - 这个其实没必要，这里保留只是用作示例，万一以后要这样实现呢！！
    int updateByIdForSafePrivate(@Param(Constants.ENTITY) T entity);
}

