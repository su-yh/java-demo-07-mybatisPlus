package com.suyh.mybatis.injector.methods;

import com.baomidou.mybatisplus.core.injector.methods.UpdateById;
import com.suyh.mybatis.safe.BaseMapperSafeX;
import com.suyh.mybatis.safe.BaseSafeUpdate;

/**
 * @author suyh
 * @since 2025-08-28
 */
public class UpdateByIdForSafeVersion extends UpdateById {
    /**
     * @see BaseMapperSafeX#updateByIdForSafePrivate(BaseSafeUpdate)  这个方法名
     */
    public static final String METHOD_NAME = "updateByIdForSafePrivate";

    public UpdateByIdForSafeVersion() {
        super(METHOD_NAME);
    }

    // 这里不需要重新实现这个注入方法，只是保留示例而以。以后需要重新实现的时候再来实现

    // @Override
    // public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
    //     // SqlMethod sqlMethod = SqlMethod.UPDATE_BY_ID_FOR_SAFE_VERSION_METHOD;
    //     String fmtSql = "UPDATE %s SET %s WHERE %s = #{%s} AND %s = #{version} %s";
    //     fmtSql = SqlMethod.UPDATE_BY_ID.getSql();   // TODO: suyh - 这里先用这个进行调试，看原代码是怎么实现的。是否有添加这个版本号
    //     final String additional = optlockVersion(tableInfo) + tableInfo.getLogicDeleteSql(true, true);
    //     String sql = String.format(fmtSql, tableInfo.getTableName(),
    //             sqlSet(tableInfo.isWithLogicDelete(), false, tableInfo, false, ENTITY, ENTITY_DOT),
    //             tableInfo.getKeyColumn(), ENTITY_DOT + tableInfo.getKeyProperty(), additional);
    //     SqlSource sqlSource = super.createSqlSource(configuration, sql, modelClass);
    //     return addUpdateMappedStatement(mapperClass, modelClass, methodName, sqlSource);
    // }
}
