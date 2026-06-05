package com.twinl.controller;

import com.twinl.service.BusinessAnalyticsService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.twinl.service.NotificationService;

@RestController
@RequestMapping("/api/v1/business-analytics")
public class BusinessAnalyticsController {

    private final BusinessAnalyticsService businessAnalyticsService;
    private final NotificationService notificationService;

    public BusinessAnalyticsController(BusinessAnalyticsService businessAnalyticsService,
                                       NotificationService notificationService) {
        this.businessAnalyticsService = businessAnalyticsService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(businessAnalyticsService.getDashboardData(startDate, endDate));
    }

    @GetMapping("/online-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> getOnlineUsers() {
        return ResponseEntity.ok(Map.of("onlineUsers", notificationService.getOnlineUserCount()));
    }
}
