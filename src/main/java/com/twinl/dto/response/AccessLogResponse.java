package com.twinl.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogResponse {
    private Long id;
    private String ipAddress;
    private String userAgent;
    private String device;
    private String location;
    private String source;
    private String status;
    private Long userId;
    private String userName;
    private String userRole;
    private LocalDateTime createdAt;
}
