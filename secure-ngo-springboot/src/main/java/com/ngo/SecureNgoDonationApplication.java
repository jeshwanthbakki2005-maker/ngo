package com.ngo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SecureNgoDonationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureNgoDonationApplication.class, args);
    }
}
