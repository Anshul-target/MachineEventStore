package com.example.MachineEventStore.service;

import com.example.MachineEventStore.BaseIntegrationTest;
import com.example.MachineEventStore.model.dto.BatchIngestionResponse;
import com.example.MachineEventStore.model.dto.EventRequest;
import com.example.MachineEventStore.repository.MachineEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventIngestionServiceTest extends BaseIntegrationTest {

    @Autowired
    private EventIngestionService eventIngestionService;

    @Autowired
    private MachineEventRepository eventRepository;

    @AfterEach
    void cleanup() {
        eventRepository.deleteAll();
    }



    @Test
    void test2_differentPayloadNewerReceivedTime_shouldUpdate() {
        // Given: First event
        EventRequest event1 = createEvent("E-2", "M-001", 1000L, 2);
        eventIngestionService.processBatch(List.of(event1));

        sleep(100);

        // Second event with same eventId but different payload
        EventRequest event2 = createEvent("E-2", "M-001", 2000L, 5);

        // When: Process second event
        BatchIngestionResponse response = eventIngestionService.processBatch(List.of(event2));

        // Then: Should be updated
        assertEquals(0, response.getAccepted());
        assertEquals(0, response.getDeduped());
        assertEquals(1, response.getUpdated(), "Different payload with newer receivedTime should update");
        assertEquals(0, response.getRejected());

        var savedEvent = eventRepository.findById("E-2").orElseThrow();
        assertEquals(2000L, savedEvent.getDurationMs());
        assertEquals(5, savedEvent.getDefectCount());
    }

    @Test
    void test3_differentPayloadOlderReceivedTime_shouldBeIgnored() {
        // Given: First event with current time
        EventRequest newerEvent = createEvent("E-3", "M-001", 2000L, 5);
        eventIngestionService.processBatch(List.of(newerEvent));

        // Second event with older receivedTime (pre-set before service processes it)
        EventRequest olderEvent = createEventWithReceivedTime(
                "E-3", "M-001", 1000L, 2,
                Instant.now().minusSeconds(60)
        );

        // When: Process older event
        BatchIngestionResponse response = eventIngestionService.processBatch(List.of(olderEvent));

        // Then: Should be deduped (ignored)
        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getDeduped(), "Older receivedTime should be ignored");
        assertEquals(0, response.getUpdated());
        assertEquals(0, response.getRejected());

        var savedEvent = eventRepository.findById("E-3").orElseThrow();
        assertEquals(2000L, savedEvent.getDurationMs(), "Original values should remain");
    }

    @Test
    void test4_invalidDuration_shouldBeRejected() {
        // Given: Events with invalid durations
        EventRequest negativeDuration = createEvent("E-4", "M-001", -100L, 0);
        EventRequest tooLongDuration = createEvent("E-5", "M-001", 25_000_000L, 0);

        // When: Process invalid events
        BatchIngestionResponse response = eventIngestionService.processBatch(
                List.of(negativeDuration, tooLongDuration)
        );

        // Then: Both should be rejected
        assertEquals(0, response.getAccepted());
        assertEquals(2, response.getRejected(), "Invalid durations should be rejected");
        assertEquals(2, response.getRejections().size());

        assertTrue(response.getRejections().stream()
                .anyMatch(r -> r.getReason().equals("INVALID_DURATION")));
    }

    @Test
    void test5_futureEventTime_shouldBeRejected() {
        // Given: Event with time > 15 minutes in future
        EventRequest futureEvent = createEventWithTime(
                "E-6", "M-001", 1000L, 0,
                Instant.now().plusSeconds(20 * 60)
        );

        // When: Process future event
        BatchIngestionResponse response = eventIngestionService.processBatch(List.of(futureEvent));

        // Then: Should be rejected
        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getRejected(), "Future event time should be rejected");
        assertEquals("FUTURE_EVENT_TIME", response.getRejections().get(0).getReason());
    }

    @Test
    void test6_defectCountMinusOne_shouldBeStoredButIgnoredInStats() {
        // Given: Event with defectCount = -1 (unknown)
        EventRequest unknownDefectEvent = createEvent("E-7", "M-001", 1000L, -1);

        // When: Process event
        BatchIngestionResponse response = eventIngestionService.processBatch(List.of(unknownDefectEvent));

        // Then: Should be accepted
        assertEquals(1, response.getAccepted(), "Event with defectCount=-1 should be accepted");

        var savedEvent = eventRepository.findById("E-7").orElseThrow();
        assertEquals(-1, savedEvent.getDefectCount());
    }

    // Helper methods
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

    private EventRequest createEventWithTimestamp(String eventId, String machineId,
                                                  Long durationMs, int defectCount, Instant timestamp) {
        return EventRequest.builder()
                .eventId(eventId)
                .eventTime(timestamp)
                .machineId(machineId)
                .durationMs(durationMs)
                .defectCount(defectCount)
                .lineId("L-01")
                .factoryId("F01")
                .build();
    }

    private EventRequest createEventWithTime(String eventId, String machineId,
                                             Long durationMs, int defectCount, Instant eventTime) {
        return EventRequest.builder()
                .eventId(eventId)
                .eventTime(eventTime)
                .machineId(machineId)
                .durationMs(durationMs)
                .defectCount(defectCount)
                .lineId("L-01")
                .factoryId("F01")
                .build();
    }

    private EventRequest createEventWithReceivedTime(String eventId, String machineId,
                                                     Long durationMs, int defectCount, Instant receivedTime) {
        EventRequest request = EventRequest.builder()
                .eventId(eventId)
                .eventTime(Instant.now())
                .machineId(machineId)
                .durationMs(durationMs)
                .defectCount(defectCount)
                .lineId("L-01")
                .factoryId("F01")
                .build();
        request.setReceivedTime(receivedTime);
        return request;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}