package com.example.MachineEventStore.controller;



import com.example.MachineEventStore.model.dto.BatchIngestionResponse;
import com.example.MachineEventStore.model.dto.EventRequest;
import com.example.MachineEventStore.service.EventIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event Ingestion", description = "APIs for ingesting machine events in batches")
public class EventController {

    private final EventIngestionService eventIngestionService;

    @PostMapping("/batch")
    @Operation(
            summary = "Ingest batch of machine events",
            description = "Process a batch of machine events with validation, deduplication, and update logic. " +
                    "Handles concurrent requests safely with atomic operations. " +
                    "Returns statistics about accepted, deduped, updated, and rejected events."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Batch processed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BatchIngestionResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful batch processing",
                                    value = """
                        {
                          "accepted": 950,
                          "deduped": 30,
                          "updated": 10,
                          "rejected": 10,
                          "rejections": [
                            {
                              "eventId": "E-99",
                              "reason": "INVALID_DURATION",
                              "message": "Duration must be between 0 and 6 hours"
                            }
                          ]
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or malformed JSON",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Malformed request",
                                    value = """
                        {
                          "timestamp": "2026-01-15T10:30:00Z",
                          "status": 400,
                          "error": "Invalid Request",
                          "message": "Request body is not readable. Please check JSON format."
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Server error",
                                    value = """
                        {
                          "timestamp": "2026-01-15T10:30:00Z",
                          "status": 500,
                          "error": "Internal Server Error",
                          "message": "An unexpected error occurred. Please try again later."
                        }
                        """
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Array of machine events to process",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EventRequest.class),
                    examples = @ExampleObject(
                            name = "Sample batch request",
                            value = """
                    [
                      {
                        "eventId": "E-1",
                        "eventTime": "2026-01-15T10:12:03.123Z",
                        "receivedTime": "2026-01-15T10:12:04.500Z",
                        "machineId": "M-001",
                        "durationMs": 4312,
                        "defectCount": 2,
                        "lineId": "L-01",
                        "factoryId": "F01"
                      },
                      {
                        "eventId": "E-2",
                        "eventTime": "2026-01-15T10:15:03.123Z",
                        "receivedTime": "2026-01-15T10:15:04.500Z",
                        "machineId": "M-001",
                        "durationMs": 1500,
                        "defectCount": -1,
                        "lineId": "L-01",
                        "factoryId": "F01"
                      }
                    ]
                    """
                    )
            )
    )
    public ResponseEntity<BatchIngestionResponse> ingestBatch(
            @Valid @RequestBody List<EventRequest> events) {

        log.info("Received batch ingestion request with {} events", events.size());

        // Validate batch is not empty
        if (events == null || events.isEmpty()) {
            log.warn("Empty batch received");
            return ResponseEntity.badRequest().build();
        }

        // Process the batch
        BatchIngestionResponse response = eventIngestionService.processBatch(events);

        log.info("Batch processing completed: accepted={}, deduped={}, updated={}, rejected={}",
                response.getAccepted(), response.getDeduped(), response.getUpdated(), response.getRejected());

        return ResponseEntity.ok(response);
    }
}
