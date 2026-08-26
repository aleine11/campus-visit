package com.campus.visit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 校园参观预约与智能咨询系统 - 启动类
 *
 * 启动后访问地址：http://localhost:8088/api
 *
 * 关键注解说明：
 * - @SpringBootApplication：SpringBoot 自动配置入口
 * - @MapperScan：扫描 MyBatis-Plus 的 Mapper 接口
 * - @EnableAsync：开启异步调用，AI 问答等耗时操作可异步处理
 */
@SpringBootApplication
@MapperScan("com.campus.visit.mapper")
@EnableAsync
public class CampusVisitApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusVisitApplication.class, args);
        System.out.println("""

                ╔══════════════════════════════════════════════════════╗
                ║   校园参观预约与智能咨询系统 启动成功                 ║
                ║   访问地址：http://localhost:8088/api                  ║
                ║   哈尔滨剑桥学院 智能科学与工程学院 毕业设计           ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
