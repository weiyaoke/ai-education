package com.tianji.learning.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableInfoContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;

@Component
public class MybatisConfiguration {
    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor(){
        HashMap<String, TableNameHandler> map = new HashMap<>();
        map.put("points_board", new TableNameHandler() {
            @Override
            public String dynamicTableName(String sql, String tableName) {
                return TableInfoContext.getInfo() != null ? TableInfoContext.getInfo() : tableName;
            }
        });
        map.put("points_record", new TableNameHandler() {
            @Override
            public String dynamicTableName(String sql, String tableName) {
                return TableInfoContext.getInfo() != null ? TableInfoContext.getInfo() : tableName;
            }
        });
        return new DynamicTableNameInnerInterceptor(map);
    }
}
