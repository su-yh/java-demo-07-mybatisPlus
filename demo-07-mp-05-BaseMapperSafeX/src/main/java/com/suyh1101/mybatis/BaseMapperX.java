package com.suyh1101.mybatis;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 在 MyBatis Plus 的 BaseMapper 的基础上拓展，提供更多的能力
 *
 * 1. {@link BaseMapper} 为 MyBatis Plus 的基础接口，提供基础的 CRUD 能力
 */
public interface BaseMapperX<T> extends BaseMapper<T> {
    default LambdaQueryWrapperX<T> build() {
        return new LambdaQueryWrapperX<>();
    }

    default PageResult<T> selectPage(PageParam pageParam, @Param("ew") Wrapper<T> queryWrapper) {
        // MyBatis Plus 查询
        IPage<T> mpPage = MyBatisUtils.buildPage(pageParam);
        selectPage(mpPage, queryWrapper);
        // 转换返回
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    default T selectOne(String field, Object value) {
        return selectOne(new QueryWrapper<T>().eq(field, value));
    }

    default T selectOne(SFunction<T, ?> field, Object value) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default T selectOne(String field1, Object value1, String field2, Object value2) {
        return selectOne(new QueryWrapper<T>().eq(field1, value1).eq(field2, value2));
    }

    default T selectOne(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2));
    }

    default T selectOne(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2,
                        SFunction<T, ?> field3, Object value3) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2)
                .eq(field3, value3));
    }

    default Long selectCount() {
        return selectCount(new QueryWrapper<>());
    }

    default Long selectCount(String field, Object value) {
        return selectCount(new QueryWrapper<T>().eq(field, value));
    }

    default Long selectCount(SFunction<T, ?> field, Object value) {
        return selectCount(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList() {
        return selectList(new QueryWrapper<>());
    }

    default List<T> selectList(String field, Object value) {
        return selectList(new QueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList(SFunction<T, ?> field, Object value) {
        return selectList(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return selectList(new QueryWrapper<T>().in(field, values));
    }

    default List<T> selectList(SFunction<T, ?> field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return selectList(new LambdaQueryWrapper<T>().in(field, values));
    }

    default List<T> selectList(SFunction<T, ?> leField, SFunction<T, ?> geField, Object value) {
        return selectList(new LambdaQueryWrapper<T>().le(leField, value).ge(geField, value));
    }

    /**
     * 批量插入，适合大量数据插入
     * 批量插入需要开启：rewriteBatchedStatements=true
     *
     * @param entities 实体们
     */
    default void insertBatch(Collection<T> entities) {
        Db.saveBatch(entities);
    }

    /**
     * 批量插入，适合大量数据插入
     * 批量插入需要开启：rewriteBatchedStatements=true
     *
     * @param entities 实体们
     * @param size     插入数量 Db.saveBatch 默认为 1000
     */
    default void insertBatch(Collection<T> entities, int size) {
        Db.saveBatch(entities, size);
    }

    default void updateBatch(T update) {
        update(update, new QueryWrapper<>());
    }

    default void updateBatch(Collection<T> entities) {
        Db.updateBatchById(entities);
    }

    default void updateBatch(Collection<T> entities, int size) {
        Db.updateBatchById(entities, size);
    }

    default void saveOrUpdateBatch(Collection<T> collection) {
        Db.saveOrUpdateBatch(collection);
    }

    // 批量插入要注意的是自增主键ID 不会回写。所以如果需要自增主键ID 的话，就不能使用该方法进行批量插入。
    void insertEntities(Collection<T> entities);

    default void insertEntitiesBatch(Collection<T> entities) {
        insertEntitiesBatch(entities, null);
    }

    default void insertEntitiesBatch(Collection<T> entities, Integer batchSize) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        // insertEntities(entitiesBatch);
        // 2. 批次大小适配（默认 1000，最小 1）
        int actualBatchSize = batchSize == null || batchSize < 1 ? 1000 : batchSize;

        // 3. 把 Collection 转为 List（方便按索引拆分）
        List<T> entityList = new ArrayList<>(entities);

        // 4. 分批循环插入
        int totalSize = entityList.size();
        // 循环次数 = 总数量 / 批次大小（向上取整）
        int totalBatches = (totalSize + actualBatchSize - 1) / actualBatchSize;

        for (int i = 0; i < totalBatches; i++) {
            // 计算当前批次的起始索引和结束索引
            int startIndex = i * actualBatchSize;
            // 结束索引 = 最小（起始索引+批次大小，总数量），避免越界
            int endIndex = Math.min(startIndex + actualBatchSize, totalSize);

            // 拆分当前批次的集合
            List<T> currentBatch = entityList.subList(startIndex, endIndex);

            // 调用你的原有批量插入方法（若需要重试/异常处理，可在这里扩展）
            insertEntities(currentBatch);

            // 可选：打印批次日志（便于调试和监控）
            System.out.printf("第 %d 批插入完成，批次大小：%d，累计插入：%d/%d%n",
                    i + 1, currentBatch.size(), endIndex, totalSize);
        }
    }
}
