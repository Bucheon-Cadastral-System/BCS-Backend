package com.is.bcs;

import com.is.bcs.config.properties.OAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(OAuthProperties.class)
@SpringBootApplication
public class BcsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BcsApplication.class, args);
    }

}
