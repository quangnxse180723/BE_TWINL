package com.twinl.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ghn")
@Getter
@Setter
public class GhnProperties {
	private String baseUrl;
	private String token;
	private Integer shopId;
	private Integer serviceTypeId;
	private Integer paymentTypeId;
	private String returnAddress;
	private String returnPhone;
	private Integer returnDistrictId;
	private String returnWardCode;
	private String webhookToken;
}
