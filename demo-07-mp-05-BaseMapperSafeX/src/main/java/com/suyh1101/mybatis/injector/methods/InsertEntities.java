package com.suyh1101.mybatis.injector.methods;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.List;
import java.util.stream.Collectors;

public class InsertEntities extends AbstractMethod {
    // SqlMethod
    private static final String METHOD = "insertEntities";
    private static final String DESC = "批量插入";
    private static final String SQL = "<script>\nINSERT INTO %s %s VALUES %s\n</script>";

    public InsertEntities() {
        super(METHOD);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 2. 构建字段部分：(id, nick_name, age)（复用框架逻辑，排除自增主键）
        List<TableFieldInfo> insertFields = tableInfo.getFieldList();   // 这个方法直接不会包含主键ID 列
        String columnSql = insertFields.stream()
                .map(TableFieldInfo::getColumn)
                .collect(Collectors.joining(StringPool.COMMA, LEFT_BRACKET, RIGHT_BRACKET));

        // 3. 构建单组值脚本：(#{item.id}, #{item.nickName}, #{item.age})
        // 关键：直接生成 #{property}，不添加 if test="null" 判断
        String singleValueScript = insertFields.stream()
                .map(field -> "#{item." + field.getProperty() + "}") // item 是 foreach 的循环变量
                .collect(Collectors.joining(StringPool.COMMA, LEFT_BRACKET, RIGHT_BRACKET));

        // 4. 构建多组值脚本：用 <foreach> 循环列表，拼接多组 ()，并用逗号分隔
        // foreach 属性说明：
        // collection="list"：参数是 List 类型，默认集合名是 list
        // item="item"：循环变量名（对应上面的 #{item.xxx}）
        // separator=","：每组值之间用逗号分隔
        String multiValueScript = SqlScriptUtils.convertForeach(
                singleValueScript, // 单组值模板
                "list", // 集合参数名（方法参数是 List，所以用 list）
                null,  // 索引变量名设为 index（与循环变量名区分）
                "item", // 循环变量名
                StringPool.COMMA // 组之间的分隔符
        );

        // 5. 最终 SQL 脚本（拼接字段和多组值）
        String sql = String.format(
                SQL,
                tableInfo.getTableName(), // 表名
                columnSql, // 字段部分 (col1, col2)
                multiValueScript // 多组值部分 (...) , (...)
        );

        // 6. 打印生成的 SQL 脚本（可选，用于调试）
        System.out.println("生成的批量插入 SQL：" + sql);

        // 7. 构建 SqlSource 并注册 MappedStatement
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, List.class);
        return this.addInsertMappedStatement(
                mapperClass, modelClass, super.methodName, sqlSource,
                NoKeyGenerator.INSTANCE, null, null
        );
    }
}
