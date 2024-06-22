package com.suyh.mp.dynamic.config;

import com.suyh.mp.dynamic.config.properties.DynamicDataSourceProviderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author suyh
 * @since 2024-03-17
 */
@EnableConfigurationProperties(DynamicDataSourceProviderProperties.class)
@Configuration
public class DynamicDataSourceConfiguration {

}
