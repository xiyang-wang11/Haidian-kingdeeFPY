package com.invoice.assistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.invoice.assistant.mapper")
public class InvoiceAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvoiceAssistantApplication.class, args);
    }
}

