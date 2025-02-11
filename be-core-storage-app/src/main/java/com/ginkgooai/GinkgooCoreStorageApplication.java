package com.ginkgooai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author: david
 * @date: 22:43 2025/2/8
 */

@SpringBootApplication
@EnableFeignClients
public class GinkgooCoreStorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(GinkgooCoreStorageApplication.class, args);
    }

}
