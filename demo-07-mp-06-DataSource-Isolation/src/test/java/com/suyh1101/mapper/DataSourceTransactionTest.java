package com.suyh1101.mapper;

/**
 * @author suyh
 * @since 2025-08-28
 */

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author suyh
 * @since 2025-08-28
 */
@Component
@AllArgsConstructor
@Slf4j
public class DataSourceTransactionTest {

    @Resource
    private DataSource dataSource;

    public void showIsolation() {
        dataSourceIsolation("default");
    }

    private void dataSourceIsolation(String name) {
        try (Connection conn = dataSource.getConnection()) {
            int isolation = conn.getTransactionIsolation();
            // 对照 java.sql.Connection 常量判断：
            // 1: READ_UNCOMMITTED, 2: READ_COMMITTED, 4: REPEATABLE_READ, 8: SERIALIZABLE
            String result = "TRANSACTION_NONE";
            switch (isolation) {
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    result = "TRANSACTION_READ_UNCOMMITTED";
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    result = "TRANSACTION_READ_COMMITTED";
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    result = "TRANSACTION_REPEATABLE_READ";
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                    result = "TRANSACTION_SERIALIZABLE";
                    break;
                case Connection.TRANSACTION_NONE:
                default:
                    break;
            }
            log.info("name: {}, 当前隔离级别：{}", name, result);
        } catch (Exception e) {
            log.error("failed.", e);
        }
    }
}

