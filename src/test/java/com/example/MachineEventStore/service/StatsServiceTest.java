package com.example.MachineEventStore.service;

import com.example.MachineEventStore.BaseIntegrationTest;

import com.example.MachineEventStore.model.dto.StatsResponse;
import com.example.MachineEventStore.model.entity.MachineEvent;
import com.example.MachineEventStore.model.enums.HealthStatus;
import com.example.MachineEventStore.repository.MachineEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class StatsServiceTest extends BaseIntegrationTest {

    @Autowired
    private StatsService statsService;

    @Autowired
    private MachineEventRepository eventRepository;

    private Instant baseTime;

    @BeforeEach
    void setup() {
        baseTime = Instant.now().truncatedTo(ChronoUnit.HOURS);
        eventRepository.deleteAll();
    }

    @AfterEach
    void cleanup() {
        eventRepository.deleteAll();
    }



    @Test
    void test8_defectCountMinusOne_excludedFromDefectCalculations() {
        // Given: Mix of events with known and unknown defects
        Instant start = baseTime;
        Instant end = baseTime.plusSeconds(3600);

        saveEvent("E-1", "M-001", start.plusSeconds(100), 1000, 5);
        saveEvent("E-2", "M-001", start.plusSeconds(200), 1000, -1);
        saveEvent("E-3", "M-001", start.plusSeconds(300), 1000, 3);
        saveEvent("E-4", "M-001", start.plusSeconds(400), 1000, -1);

        // When: Query stats
        StatsResponse stats = statsService.getStats("M-001", start, end);

        // Then
        assertEquals(4, stats.getEventsCount(), "All events should be counted");
        assertEquals(8, stats.getDefectsCount(), "Only known defects counted (5+3, excluding -1)");

        assertEquals(8.0, stats.getAvgDefectRate(), 0.01);
        assertEquals(HealthStatus.WARNING, stats.getStatus(), "8.0 > 2.0 threshold");
    }

    private void saveEvent(String eventId, String machineId, Instant eventTime,
                           long durationMs, int defectCount) {
        MachineEvent event = MachineEvent.builder()
                .eventId(eventId)
                .eventTime(eventTime)
                .receivedTime(Instant.now())
                .machineId(machineId)
                .durationMs(durationMs)
                .defectCount(defectCount)
                .lineId("L-01")
                .factoryId("F01")
                .build();
        eventRepository.save(event);
    }
}