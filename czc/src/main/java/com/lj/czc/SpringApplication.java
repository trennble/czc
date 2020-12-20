package com.lj.czc;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SpringApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(SpringApplication.class).run(args);
    }
}