package com.example.MachineEventStore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopDefectLineResponse {

    private String lineId;
    private long totalDefects;
    private long eventCount;
    private double defectsPercent; // Defects per 100 events, rounded to 2 decimals
}