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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private EventIngestionService eventIngestionService;

    @Autowired
    private MachineEventRepository eventRepository;

    @AfterEach
    void cleanup() {
        eventRepository.deleteAll();
    }

    @Test
    void test9_concurrentIngestion_shouldNotCorruptCounts() throws InterruptedException, ExecutionException {
        // Given: 10 threads, each sending 10 events concurrently
        int threadCount = 10;
        int eventsPerThread = 10;
        int totalEvents = threadCount * eventsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<BatchIngestionResponse>> futures = new ArrayList<>();

        // When: Submit concurrent batch ingestion tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Future<BatchIngestionResponse> future = executor.submit(() -> {
                List<EventRequest> events = new ArrayList<>();
                for (int j = 0; j < eventsPerThread; j++) {
                    String eventId = String.format("E-%d-%d", threadId, j);
                    events.add(createEvent(eventId, "M-001", 1000L, 1));
                }
                return eventIngestionService.processBatch(events);
            });
            futures.add(future);
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Collect results
        int totalAccepted = 0;
        int totalRejected = 0;

        for (Future<BatchIngestionResponse> future : futures) {
            BatchIngestionResponse response = future.get();
            totalAccepted += response.getAccepted();
            totalRejected += response.getRejected();
        }

        // Then: All events should be accounted for
        assertEquals(totalEvents, totalAccepted + totalRejected,
                "Total accepted + rejected should equal total events");

        long dbCount = eventRepository.count();
        assertEquals(totalAccepted, dbCount,
                "Database count should match accepted events");

        System.out.println("Concurrency Test Results:");
        System.out.println("  Threads: " + threadCount);
        System.out.println("  Events per thread: " + eventsPerThread);
        System.out.println("  Total events: " + totalEvents);
        System.out.println("  Accepted: " + totalAccepted);
        System.out.println("  Rejected: " + totalRejected);
        System.out.println("  DB Count: " + dbCount);
    }

    @Test
    void test10_concurrentUpdates_sameEventId_shouldHandleCorrectly() throws InterruptedException, ExecutionException {
        // Given: Same eventId sent from multiple threads
        String eventId = "E-CONCURRENT";
        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<BatchIngestionResponse>> futures = new ArrayList<>();

        // When: Multiple threads try to update same event simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int defectCount = i;
            Future<BatchIngestionResponse> future = executor.submit(() -> {
                startLatch.await();
                EventRequest event = createEvent(eventId, "M-001", 1000L + defectCount, defectCount);
                return eventIngestionService.processBatch(List.of(event));
            });
            futures.add(future);
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Collect results
        int totalAccepted = 0;
        int totalUpdated = 0;
        int totalDeduped = 0;

        for (Future<BatchIngestionResponse> future : futures) {
            BatchIngestionResponse response = future.get();
            totalAccepted += response.getAccepted();
            totalUpdated += response.getUpdated();
            totalDeduped += response.getDeduped();
        }

        // Then
        assertEquals(1, totalAccepted, "Exactly one should be accepted");
        assertEquals(threadCount - 1, totalUpdated + totalDeduped,
                "Rest should be updates or dedupes");
        assertEquals(1, eventRepository.count(),
                "Database should have exactly one event");

        System.out.println("Concurrent Update Test Results:");
        System.out.println("  Threads: " + threadCount);
        System.out.println("  Accepted: " + totalAccepted);
        System.out.println("  Updated: " + totalUpdated);
        System.out.println("  Deduped: " + totalDeduped);
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