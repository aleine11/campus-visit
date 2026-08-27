package com.campus.visit.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 *
 * 本类做三件事：
 * 1. 注册分页插件 —— 业务调用 page(...) 自动加 LIMIT
 * 2. 注册乐观锁插件 —— @Version 字段的 entity，updateById 自动带上 version 条件
 * 3. 注册自动填充处理器 —— @TableField(fill=INSERT) 的字段插入时自动赋值
 *
 * 乐观锁防超卖原理（visit_session.used_people）：
 * 用户 A 和 B 同时抢最后 1 个名额：
 * ┌────────────────────────────────────────────────────────┐
 * │ A: UPDATE ... SET used_people=1, version=1 │
 * │ WHERE id=1 AND version=0 AND used_people+1<=50 │ → 影响 1 行，成功
 * │ B: UPDATE ... SET used_people=2, version=2 │
 * │ WHERE id=1 AND version=1 AND used_people+1<=50 │ → 影响 0 行，失败
 * └────────────────────────────────────────────────────────┘
 * B 检测到失败，提示"名额已被抢完"，业务层回滚
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MP 拦截器链：分页 + 乐观锁
     * 拦截器按添加顺序执行，乐观锁需要在 update 语句执行前拦截，放后面也行
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件 —— 必须在第一位，否则乐观锁的 version 条件可能被分页包装干扰
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁插件 —— 遇到带 @Version 的 entity，updateById 自动拼接 version 条件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * 自动填充处理器
     *
     * 配合 entity 上的 @TableField(fill=...) 注解使用：
     * 
     * @TableField(fill = FieldFill.INSERT) ← 仅插入时填充
     * @TableField(fill = FieldFill.INSERT_UPDATE) ← 插入和更新都填充
     *
     *                  数据库表的 create_time / update_time 都有 DEFAULT
     *                  CURRENT_TIMESTAMP，
     *                  但 MP 的自动填充能保证 Java 层也有一致的时间值，避免不同数据库时区差异
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 插入时：填充 createTime 和 updateTime（当前时间）
                LocalDateTime now = LocalDateTime.now();
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 更新时：只填充 updateTime
                LocalDateTime now = LocalDateTime.now();
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
            }
        };
    }
}
