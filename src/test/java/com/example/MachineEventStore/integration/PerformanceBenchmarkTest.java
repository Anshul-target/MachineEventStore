package com.example.MachineEventStore.integration;



import com.example.MachineEventStore.BaseIntegrationTest;
import com.example.MachineEventStore.model.dto.BatchIngestionResponse;
import com.example.MachineEventStore.model.dto.EventRequest;
import com.example.MachineEventStore.repository.MachineEventRepository;
import com.example.MachineEventStore.service.EventIngestionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceBenchmarkTest extends BaseIntegrationTest {

    @Autowired
    private EventIngestionService eventIngestionService;

    @Autowired
    private MachineEventRepository eventRepository;

    @AfterEach
    void cleanup() {
        eventRepository.deleteAll();
    }

    @Test
    void benchmarkBatchIngestion1000Events() {
        // Given: 1000 unique events
        List<EventRequest> events = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            events.add(createEvent("E-BENCH-" + i, "M-001", 5000L, 2));
        }

        // Warmup
        eventIngestionService.processBatch(events.subList(0, 100));
        eventRepository.deleteAll();

        // When: Process 1000 events and measure time
        long startTime = System.currentTimeMillis();
        BatchIngestionResponse response = eventIngestionService.processBatch(events);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: Verify results
        assertEquals(1000, response.getAccepted(), "All events should be accepted");
        assertEquals(0, response.getRejected(), "No events should be rejected");

        // Print results
        System.out.println("\n========================================");
        System.out.println("BENCHMARK RESULTS - 1000 Events");
        System.out.println("========================================");
        System.out.println("Total Events: 1000");
        System.out.println("Time Taken: " + duration + " ms");
        System.out.println("Throughput: " + (1000 * 1000 / duration) + " events/sec");
        System.out.println("========================================\n");

        assertTrue(duration < 1000,
                String.format("Expected < 1000ms, but took %dms", duration));
    }

    private EventRequest createEvent(String eventId, String machineId, Long durationMs, int defectCount) {
        return EventRequest.builder()
                .eventId(eventId)
                .eventTime(Instant.now())
                .machineId(machineId)
                .durationMs(durationMs)
                .defectCount(defectCount)
                .lineId("L-01")
                .factoryId("F01")
                .build();
    }
}