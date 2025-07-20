package com.ai.digitTeacher.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.ai.digitTeacher.mapper")
public class MybatisPlusConfig {
}
