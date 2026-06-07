package com.twinl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sepay")
public class SepayProperties {
    private String webhookToken;
    private String bankId;
    private String accountNumber;
    private String accountName;
}
