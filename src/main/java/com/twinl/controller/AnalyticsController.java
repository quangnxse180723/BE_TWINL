package com.twinl.controller;

import com.twinl.entity.AccessLog;
import com.twinl.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.twinl.dto.response.AccessLogResponse;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/track")
    public ResponseEntity<Void> trackVisit(HttpServletRequest request) {
        analyticsService.logAccess(request, "VISIT", null);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(analyticsService.getAnalyticsOverview(startDate, endDate));
    }

    @GetMapping("/traffic-chart")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getTrafficChart(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(analyticsService.getTrafficChartData(startDate, endDate));
    }

    @GetMapping("/top-sources")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getTopSources(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(analyticsService.getTopSources(startDate, endDate));
    }

    @GetMapping("/access-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccessLogResponse>> getAccessLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String userType) {
        return ResponseEntity.ok(analyticsService.getRecentLogs(startDate, endDate, userType));
    }
}
