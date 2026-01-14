package com.suyh.mybatis.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.suyh.mybatis.injector.methods.CreateTableIfNotExists;

import java.util.List;

/**
 * @author suyh
 * @since 2025-08-28
 */
public class GlobalSqlInjector extends DefaultSqlInjector {
    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
        methodList.add(new CreateTableIfNotExists());
        return methodList;
    }
}

