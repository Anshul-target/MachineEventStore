package com.example.MachineEventStore.controller;
import com.example.MachineEventStore.model.dto.StatsResponse;
import com.example.MachineEventStore.model.dto.TopDefectLineResponse;
import com.example.MachineEventStore.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistics", description = "APIs for querying machine and production line statistics")
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    @Operation(
            summary = "Get machine statistics",
            description = "Retrieve statistics for a specific machine within a time window. " +
                    "Time range is start (inclusive) to end (exclusive). " +
                    "Returns event count, defect count, average defect rate, and health status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StatsResponse.class),
                            examples = @ExampleObject(
                                    name = "Machine statistics example",
                                    value = """
                        {
                          "machineId": "M-001",
                          "start": "2026-01-15T00:00:00Z",
                          "end": "2026-01-15T06:00:00Z",
                          "eventsCount": 1200,
                          "defectsCount": 6,
                          "avgDefectRate": 1.0,
                          "status": "HEALTHY"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameters (missing required params, invalid time range, etc.)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid time range",
                                    value = """
                        {
                          "timestamp": "2026-01-15T10:30:00Z",
                          "status": 400,
                          "error": "Invalid Argument",
                          "message": "Invalid time range: start must be before end"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<StatsResponse> getStats(
            @Parameter(
                    description = "Machine ID to query statistics for",
                    required = true,
                    example = "M-001"
            )
            @RequestParam String machineId,

            @Parameter(
                    description = "Start of time window (inclusive) in ISO-8601 format",
                    required = true,
                    example = "2026-01-15T00:00:00Z"
            )
            @RequestParam

            Instant start,

            @Parameter(
                    description = "End of time window (exclusive) in ISO-8601 format",
                    required = true,
                    example = "2026-01-15T06:00:00Z"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end) {

        log.info("Received stats request for machine {} from {} to {}", machineId, start, end);

        // Delegate to service
        StatsResponse response = statsService.getStats(machineId, start, end);

        log.info("Stats retrieved for machine {}: events={}, defects={}, status={}",
                machineId, response.getEventsCount(), response.getDefectsCount(), response.getStatus());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-defect-lines")
    @Operation(
            summary = "Get top defect production lines",
            description = "Retrieve production lines ranked by defect percentage for a factory. " +
                    "Returns lines sorted by defects per 100 events (highest first). " +
                    "Excludes events with unknown defect count (defectCount = -1)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Top defect lines retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TopDefectLineResponse.class),
                            examples = @ExampleObject(
                                    name = "Top defect lines example",
                                    value = """
                        [
                          {
                            "lineId": "L-02",
                            "totalDefects": 45,
                            "eventCount": 150,
                            "defectsPercent": 30.00
                          },
                          {
                            "lineId": "L-01",
                            "totalDefects": 30,
                            "eventCount": 200,
                            "defectsPercent": 15.00
                          },
                          {
                            "lineId": "L-03",
                            "totalDefects": 10,
                            "eventCount": 100,
                            "defectsPercent": 10.00
                          }
                        ]
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameters",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Invalid limit",
                                    value = """
                        {
                          "timestamp": "2026-01-15T10:30:00Z",
                          "status": 400,
                          "error": "Invalid Argument",
                          "message": "Limit must be positive"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<List<TopDefectLineResponse>> getTopDefectLines(
            @Parameter(
                    description = "Factory ID to query production lines for",
                    required = true,
                    example = "F01"
            )
            @RequestParam String factoryId,

            @Parameter(
                    description = "Start of time window (inclusive) in ISO-8601 format",
                    required = true,
                    example = "2026-01-15T00:00:00Z"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @Parameter(
                    description = "End of time window (exclusive) in ISO-8601 format",
                    required = true,
                    example = "2026-01-16T00:00:00Z"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @Parameter(
                    description = "Maximum number of lines to return",
                    required = false,
                    example = "10"
            )
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Received top defect lines request for factory {} from {} to {} (limit={})",
                factoryId, from, to, limit);

        // Delegate to service
        List<TopDefectLineResponse> response = statsService.getTopDefectLines(factoryId, from, to, limit);

        log.info("Retrieved {} top defect lines for factory {}", response.size(), factoryId);

        return ResponseEntity.ok(response);
    }
}