package com.example.MachineEventStore.model.dto;


import com.example.MachineEventStore.model.enums.HealthStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsResponse {

    private String machineId;

    private Instant start;

    private Instant end;

    private long eventsCount;
    private long defectsCount;
    private double avgDefectRate;
    private HealthStatus status;
}