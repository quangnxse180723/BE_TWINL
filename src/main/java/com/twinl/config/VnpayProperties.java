package com.twinl.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnpayProperties {
	private String tmnCode;
	private String hashSecret;
	private String payUrl;
	private String returnUrl;
	private String ipnUrl;
	private String version;
	private String command;
}
