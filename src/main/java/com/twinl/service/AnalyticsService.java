package com.twinl.service;

import com.twinl.entity.AccessLog;
import com.twinl.repository.AccessLogRepository;
import com.twinl.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import com.twinl.dto.response.AccessLogResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final AccessLogRepository accessLogRepository;
    private final UserRepository userRepository;

    public AnalyticsService(AccessLogRepository accessLogRepository, UserRepository userRepository) {
        this.accessLogRepository = accessLogRepository;
        this.userRepository = userRepository;
    }

    public void logAccess(HttpServletRequest request, String status, Long userId) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        String device = parseDevice(userAgent);
        String referer = request.getHeader("Referer");
        String source = parseSource(referer);

        final String finalIp = ipAddress;
        
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            String location = getLocationFromIp(finalIp);
            AccessLog log = AccessLog.builder()
                    .ipAddress(finalIp)
                    .userAgent(userAgent)
                    .device(device)
                    .source(source)
                    .location(location)
                    .status(status)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            accessLogRepository.save(log);
        });
    }

    private String getLocationFromIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress) || ipAddress.startsWith("192.168.")) {
            return "Localhost, VN";
        }
        try {
            if (ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "http://ip-api.com/json/" + ipAddress;
            java.util.Map<String, Object> response = restTemplate.getForObject(url, java.util.Map.class);
            if (response != null && "success".equals(response.get("status"))) {
                return response.get("city") + ", " + response.get("countryCode");
            }
        } catch (Exception e) {
            // Ignore exception
        }
        return "Unknown";
    }

    private String parseSource(String referer) {
        if (referer == null || referer.isEmpty()) return "Trực tiếp";
        String lower = referer.toLowerCase();
        if (lower.contains("facebook.com") || lower.contains("twitter.com") || lower.contains("instagram.com")) {
            return "Mạng xã hội";
        }
        if (lower.contains("google.com") || lower.contains("bing.com")) {
            return "Tìm kiếm tự nhiên";
        }
        return "Trực tiếp";
    }

    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.toLowerCase().contains("mobile")) {
            if (userAgent.toLowerCase().contains("iphone") || userAgent.toLowerCase().contains("ipad")) {
                return "Mobile (iOS)";
            }
            return "Mobile (Android)";
        }
        if (userAgent.toLowerCase().contains("mac")) return "Desktop (Mac)";
        if (userAgent.toLowerCase().contains("win")) return "Desktop (Windows)";
        return "Desktop";
    }

    private LocalDateTime parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isEmpty()) {
            return isStart ? LocalDateTime.now().minusDays(30) : LocalDateTime.now();
        }
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            return isStart ? date.atStartOfDay() : date.atTime(java.time.LocalTime.MAX);
        } catch (Exception e) {
            return isStart ? LocalDateTime.now().minusDays(30) : LocalDateTime.now();
        }
    }

    public Map<String, Object> getAnalyticsOverview(String start, String end) {
        LocalDateTime startDate = parseDate(start, true);
        LocalDateTime endDate = parseDate(end, false);
        
        long totalVisits = accessLogRepository.countByCreatedAtBetween(startDate, endDate);
        long newSignups = userRepository.count(); 
        long activeUsers = accessLogRepository.countByStatusAndCreatedAtBetween("SUCCESS", startDate, endDate);

        double bounceRateVal = 0.0;
        if (totalVisits > 0) {
            long visitOnly = accessLogRepository.countByStatusAndCreatedAtBetween("VISIT", startDate, endDate);
            bounceRateVal = (double) visitOnly / totalVisits * 100;
        }
        String bounceRate = String.format("%.1f%%", bounceRateVal);

        Map<String, Object> data = new HashMap<>();
        data.put("totalVisits", totalVisits);
        data.put("activeUsers", activeUsers);
        data.put("bounceRate", bounceRate);
        data.put("newSignups", newSignups);
        return data;
    }

    public List<Map<String, Object>> getTrafficChartData(String start, String end) {
        LocalDateTime startDate = parseDate(start, true);
        LocalDateTime endDate = parseDate(end, false);
        List<AccessLog> logs = accessLogRepository.findAllByCreatedAtBetween(startDate, endDate);
        
        Map<String, Long> countByDate = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> String.format("%d Thg %d", log.getCreatedAt().getDayOfMonth(), log.getCreatedAt().getMonthValue()),
                        Collectors.counting()
                ));
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate());
        if (daysBetween < 0 || daysBetween > 100) daysBetween = 30;

        List<Map<String, Object>> chartData = new java.util.ArrayList<>();
        for (long i = daysBetween; i >= 0; i--) {
            LocalDateTime day = endDate.minusDays(i);
            String name = String.format("%d Thg %d", day.getDayOfMonth(), day.getMonthValue());
            long uv = countByDate.getOrDefault(name, 0L);
            Map<String, Object> point = new HashMap<>();
            point.put("name", name);
            point.put("uv", uv);
            chartData.add(point);
        }
        return chartData;
    }

    public List<Map<String, Object>> getTopSources(String start, String end) {
        LocalDateTime startDate = parseDate(start, true);
        LocalDateTime endDate = parseDate(end, false);
        List<AccessLog> logs = accessLogRepository.findAllByCreatedAtBetween(startDate, endDate);

        long totalLogs = logs.size();
        if (totalLogs == 0) return new java.util.ArrayList<>();

        Map<String, Long> countBySource = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getSource() != null ? log.getSource() : "Trực tiếp",
                        Collectors.counting()
                ));

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        String[] colors = {"#16a34a", "#0284c7", "#475569", "#6b7280"};
        int i = 0;
        for (Map.Entry<String, Long> entry : countBySource.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("label", entry.getKey());
            long percent = Math.round((double) entry.getValue() / totalLogs * 100);
            map.put("val", percent + "%");
            map.put("color", colors[i % colors.length]);
            result.add(map);
            i++;
        }
        result.sort((a, b) -> Integer.compare(
                Integer.parseInt(b.get("val").toString().replace("%", "")),
                Integer.parseInt(a.get("val").toString().replace("%", ""))
        ));
        return result;
    }

    public List<AccessLogResponse> getRecentLogs(String start, String end, String userType) {
        LocalDateTime startDate = parseDate(start, true);
        LocalDateTime endDate = parseDate(end, false);
        List<AccessLog> logs = accessLogRepository.findAllByCreatedAtBetween(startDate, endDate);

        // Sort from newest to oldest
        logs.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        if (userType != null && !userType.isEmpty() && !"ALL".equalsIgnoreCase(userType)) {
            if ("GUEST".equalsIgnoreCase(userType)) {
                logs = logs.stream().filter(log -> log.getUserId() == null).collect(Collectors.toList());
            } else if ("USER".equalsIgnoreCase(userType)) {
                logs = logs.stream().filter(log -> {
                    if (log.getUserId() == null) return false;
                    com.twinl.entity.User u = userRepository.findById(log.getUserId()).orElse(null);
                    if (u == null) return false;
                    boolean isStaff = u.getRoles().stream().anyMatch(r -> r.getName().name().equals("ADMIN") || r.getName().name().equals("STAFF"));
                    return !isStaff;
                }).collect(Collectors.toList());
            } else if ("STAFF".equalsIgnoreCase(userType) || "ADMIN".equalsIgnoreCase(userType)) {
                logs = logs.stream().filter(log -> {
                    if (log.getUserId() == null) return false;
                    com.twinl.entity.User u = userRepository.findById(log.getUserId()).orElse(null);
                    if (u == null) return false;
                    return u.getRoles().stream().anyMatch(r -> r.getName().name().equals("ADMIN") || r.getName().name().equals("STAFF"));
                }).collect(Collectors.toList());
            }
        }

        return logs.stream().map(log -> {
            AccessLogResponse.AccessLogResponseBuilder builder = AccessLogResponse.builder()
                    .id(log.getId())
                    .ipAddress(log.getIpAddress())
                    .userAgent(log.getUserAgent())
                    .device(log.getDevice())
                    .location(log.getLocation())
                    .source(log.getSource())
                    .status(log.getStatus())
                    .userId(log.getUserId())
                    .createdAt(log.getCreatedAt());

            if (log.getUserId() != null) {
                com.twinl.entity.User u = userRepository.findById(log.getUserId()).orElse(null);
                if (u != null) {
                    builder.userName(u.getDisplayName() != null ? u.getDisplayName() : "N/A");
                    String rolesStr = u.getRoles().stream()
                            .map(r -> r.getName().name())
                            .collect(Collectors.joining(", "));
                    builder.userRole(rolesStr);
                } else {
                    builder.userName("N/A");
                    builder.userRole("UNKNOWN");
                }
            } else {
                builder.userName("Khách");
                builder.userRole("GUEST");
            }
            return builder.build();
        }).collect(Collectors.toList());
    }
}
