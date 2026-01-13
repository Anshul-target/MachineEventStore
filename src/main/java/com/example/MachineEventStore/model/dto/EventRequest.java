package com.example.MachineEventStore.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotNull(message = "Event time is required")
       private Instant eventTime;

    // receivedTime is sent by client but will be overridden by server
      private Instant receivedTime;

    @NotBlank(message = "Machine ID is required")
    private String machineId;

    @NotNull(message = "Duration is required")
    private Long durationMs;

    @NotNull(message = "Defect count is required")
    private Integer defectCount;

    @NotBlank(message = "Line ID is required")
    private String lineId;

    @NotBlank(message = "Factory ID is required")
    private String factoryId;
}