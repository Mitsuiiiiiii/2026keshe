package com.campuslink.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * 统一 Java 8 时间类型的 JSON 序列化/反序列化格式。
 *
 * <p>spring.jackson.date-format 仅对 java.util.Date 生效，对 LocalDateTime 等 JSR-310
 * 类型无效（默认走 ISO，如 2026-06-28T23:40:00）。前端传 "yyyy-MM-dd HH:mm:ss"（空格）会反序列化失败。
 * 这里显式为 LocalDateTime/LocalDate/LocalTime 指定常用格式，输入输出保持一致。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeCustomizer() {
        return builder -> {
            builder.serializers(
                    new LocalDateTimeSerializer(DATE_TIME),
                    new LocalDateSerializer(DATE),
                    new LocalTimeSerializer(TIME)
            );
            builder.deserializers(
                    new LocalDateTimeDeserializer(DATE_TIME),
                    new LocalDateDeserializer(DATE),
                    new LocalTimeDeserializer(TIME)
            );
        };
    }
}
