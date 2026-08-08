package com.is.bcs.config.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ImageUploadProperties.class)
public class ImageUploadConfig {

}