package com.leomaster.mp.typehandler.enlist;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author suyh
 * @since 2024-09-03
 */
public abstract class AbstractEnumListTypeHandler<E extends Enum<E>> extends BaseTypeHandler<List<E>> {
    private static final String COMMA = ",";

    private static final Map<String, String> TABLE_METHOD_OF_ENUM_TYPES = new ConcurrentHashMap<>();
    private static final ReflectorFactory REFLECTOR_FACTORY = new DefaultReflectorFactory();
    private final Class<E> enumClassType;
    private final Class<String> propertyType;
    private final Invoker getInvoker;

    public AbstractEnumListTypeHandler(Class<E> enumClassType) {
        if (enumClassType == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.enumClassType = enumClassType;
        MetaClass metaClass = MetaClass.forClass(enumClassType, REFLECTOR_FACTORY);
        String name = "value";
        if (!IEnum.class.isAssignableFrom(enumClassType)) {
            name = findEnumValueFieldName(this.enumClassType).orElseThrow(() -> new IllegalArgumentException(String.format("Could not find @EnumValue in Class: %s.", this.enumClassType.getName())));
        }
        this.propertyType = String.class;
        this.getInvoker = metaClass.getGetInvoker(name);
    }

    /**
     * 查找标记标记EnumValue字段
     *
     * @param clazz class
     * @return EnumValue字段
     * @since 3.3.1
     */
    public static Optional<String> findEnumValueFieldName(Class<?> clazz) {
        if (clazz != null && clazz.isEnum()) {
            String className = clazz.getName();
            return Optional.ofNullable(CollectionUtils.computeIfAbsent(TABLE_METHOD_OF_ENUM_TYPES, className, key -> {
                Optional<Field> fieldOptional = findEnumValueAnnotationField(clazz);
                return fieldOptional.map(Field::getName).orElse(null);
            }));
        }
        return Optional.empty();
    }

    private static Optional<Field> findEnumValueAnnotationField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(EnumValue.class)).findFirst();
    }

    /**
     * 判断是否为MP枚举处理
     *
     * @param clazz class
     * @return 是否为MP枚举处理
     * @since 3.3.1
     */
    public static boolean isMpEnums(Class<?> clazz) {
        return clazz != null && clazz.isEnum() && (IEnum.class.isAssignableFrom(clazz) || findEnumValueFieldName(clazz).isPresent());
    }

    private E valueOf(String value) {
        E[] es = this.enumClassType.getEnumConstants();
        return Arrays.stream(es).filter((e) -> equalsValue(value, getValue(e))).findAny().orElse(null);
    }

    /**
     * 值比较
     *
     * @param sourceValue 数据库字段值
     * @param targetValue 当前枚举属性值
     * @return 是否匹配
     * @since 3.3.0
     */
    protected boolean equalsValue(Object sourceValue, Object targetValue) {
        String sValue = StringUtils.toStringTrim(sourceValue);
        String tValue = StringUtils.toStringTrim(targetValue);
        if (sourceValue instanceof Number && targetValue instanceof Number
                && new BigDecimal(sValue).compareTo(new BigDecimal(tValue)) == 0) {
            return true;
        }
        return Objects.equals(sValue, tValue);
    }

    private Object getValue(E e) {
        try {
            return this.getInvoker.invoke(e, new Object[0]);
        } catch (ReflectiveOperationException ex) {
            throw ExceptionUtils.mpe(ex);
        }
    }

    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int i, List<E> enList, JdbcType jdbcType) throws SQLException {
        if (enList == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int ind = 0; ind < enList.size(); ind++) {
            E en = enList.get(ind);
            Object obj = this.getValue(en);
            if (obj != null) {
                sb.append(obj);
            }
            if (ind == enList.size() - 1) {
                break;
            }

            sb.append(COMMA);
        }

        String value = sb.toString();
        if (jdbcType == null) {
            ps.setObject(i, value);
        } else {
            // see r3589
            ps.setObject(i, value, jdbcType.TYPE_CODE);
        }
    }

    @Override
    public List<E> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getObject(columnName, this.propertyType);
        if (null == value || rs.wasNull()) {
            return null;
        }

        return this.valueList(value);
    }

    @Override
    public List<E> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getObject(columnIndex, this.propertyType);
        if (null == value || rs.wasNull()) {
            return null;
        }
        return this.valueList(value);
    }

    @Override
    public List<E> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getObject(columnIndex, this.propertyType);
        if (null == value || cs.wasNull()) {
            return null;
        }
        return this.valueList(value);
    }

    private List<E> valueList(String value) {
        String[] split = value.split(COMMA);
        List<E> enList = new ArrayList<>();
        for (String ele : split) {
            E en = this.valueOf(ele);
            if (en != null) {
                enList.add(en);
            }
        }

        return enList;
    }
}
