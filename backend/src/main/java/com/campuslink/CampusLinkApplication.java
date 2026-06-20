package com.campuslink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CampusLink 校园竞赛组队与协作平台 启动类。
 */
@SpringBootApplication
@MapperScan("com.campuslink.mapper")
public class CampusLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLinkApplication.class, args);
    }
}
