package com.oilcollection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    // Collection Points
    private Long totalPoints;
    private Long activePoints;
    private Long fullPoints;
    private Long pendingApprovalPoints;

    // Schedules
    private Long totalSchedules;
    private Long pendingSchedules;
    private Long confirmedSchedules;
    private Long todaySchedules;
    private Long overdueSchedules;
    private Long inProgressSchedules;
    private Long completedSchedules;

    // Records
    private Long totalRecords;
    private Long pendingVerification;
    private Long monthRecords;
    private BigDecimal totalVolume;
    private BigDecimal monthVolume;

    // Users
    private Long totalCollectors;
    private Long activeCollectors;
    private Long totalEstablishments;
    private Long activeEstablishments;

    // Collector specific
    private Double avgQuality;
}