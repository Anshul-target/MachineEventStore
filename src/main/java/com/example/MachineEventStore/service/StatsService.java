package com.example.MachineEventStore.service;




import com.example.MachineEventStore.model.dto.StatsResponse;
import com.example.MachineEventStore.model.dto.TopDefectLineResponse;
import com.example.MachineEventStore.model.entity.MachineEvent;
import com.example.MachineEventStore.model.enums.HealthStatus;
import com.example.MachineEventStore.repository.MachineEventRepository;
import com.example.MachineEventStore.util.DateTimeUtil;
import com.example.MachineEventStore.util.EventValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final MachineEventRepository eventRepository;
    private final DateTimeUtil dateTimeUtil;
    private final EventValidator eventValidator;

    private static final double HEALTH_THRESHOLD = 2.0; // Defects per hour threshold

    /**
     * Get statistics for a specific machine in a time window
     *
     * Time window: start (inclusive) to end (exclusive)
     * Excludes events with defectCount = -1 from defect calculations
     *
     * @param machineId the machine to query
     * @param start start of time window (inclusive)
     * @param end end of time window (exclusive)
     * @return StatsResponse with aggregated statistics
     */
    public StatsResponse getStats(String machineId, Instant start, Instant end) {
        log.info("Fetching stats for machine {} from {} to {}", machineId, start, end);

        // Validate time range
        if (!dateTimeUtil.isValidTimeRange(start, end)) {
            throw new IllegalArgumentException("Invalid time range: start must be before end");
        }

        // Calculate window duration in hours
        double windowHours = dateTimeUtil.calculateHoursBetween(start, end);

        // Query 1: Get total event count (includes defectCount = -1)
        long eventsCount = eventRepository.countByMachineIdAndEventTimeBetween(machineId, start, end);

        // Query 2: Get events excluding unknown defects (defectCount != -1)
        List<MachineEvent> eventsForDefectCalculation = eventRepository
                .findByMachineIdAndEventTimeRangeExcludingUnknownDefects(machineId, start, end);

        // Calculate total defects (sum of defectCount from filtered events)
        long defectsCount = eventsForDefectCalculation.stream()
                .mapToLong(MachineEvent::getDefectCount)
                .sum();

        // Calculate average defect rate per hour
        double avgDefectRate = dateTimeUtil.calculateDefectRate(defectsCount, windowHours);

        // Determine health status based on threshold
        HealthStatus status = avgDefectRate < HEALTH_THRESHOLD
                ? HealthStatus.HEALTHY
                : HealthStatus.WARNING;

        log.info("Stats for machine {}: events={}, defects={}, rate={}, status={}",
                machineId, eventsCount, defectsCount, avgDefectRate, status);

        return StatsResponse.builder()
                .machineId(machineId)
                .start(start)
                .end(end)
                .eventsCount(eventsCount)
                .defectsCount(defectsCount)
                .avgDefectRate(avgDefectRate)
                .status(status)
                .build();
    }

    /**
     * Get top defect lines for a factory in a time window
     *
     * Returns production lines ranked by defect percentage
     * Excludes events with defectCount = -1 from calculations
     *
     * @param factoryId the factory to query
     * @param from start of time window (inclusive)
     * @param to end of time window (exclusive)
     * @param limit maximum number of lines to return
     * @return List of TopDefectLineResponse sorted by defectsPercent descending
     */
    public List<TopDefectLineResponse> getTopDefectLines(
            String factoryId,
            Instant from,
            Instant to,
            int limit) {

        log.info("Fetching top {} defect lines for factory {} from {} to {}",
                limit, factoryId, from, to);

        // Validate time range
        if (!dateTimeUtil.isValidTimeRange(from, to)) {
            throw new IllegalArgumentException("Invalid time range: from must be before to");
        }

        // Validate limit
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        // Query: Get all events for factory excluding unknown defects
        List<MachineEvent> events = eventRepository
                .findByFactoryIdAndEventTimeRangeExcludingUnknownDefects(factoryId, from, to);

        // Group events by lineId and calculate statistics
        Map<String, LineStats> lineStatsMap = new HashMap<>();

        for (MachineEvent event : events) {
            String lineId = event.getLineId();

            // Get or create stats for this line
            LineStats stats = lineStatsMap.computeIfAbsent(
                    lineId,
                    k -> new LineStats(lineId)
            );

            // Update statistics
            stats.incrementEventCount();
            stats.addDefects(event.getDefectCount());
        }

        // Convert to response DTOs and calculate defect percentages
        List<TopDefectLineResponse> responses = lineStatsMap.values().stream()
                .map(stats -> {
                    double defectsPercent = dateTimeUtil.calculateDefectsPercent(
                            stats.getTotalDefects(),
                            stats.getEventCount()
                    );

                    return TopDefectLineResponse.builder()
                            .lineId(stats.getLineId())
                            .totalDefects(stats.getTotalDefects())
                            .eventCount(stats.getEventCount())
                            .defectsPercent(defectsPercent)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getDefectsPercent(), a.getDefectsPercent())) // Descending
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Found {} lines with defects for factory {}", responses.size(), factoryId);

        return responses;
    }

    /**
     * Inner class to hold line statistics during aggregation
     */
    private static class LineStats {
        private final String lineId;
        private long eventCount;
        private long totalDefects;

        public LineStats(String lineId) {
            this.lineId = lineId;
            this.eventCount = 0;
            this.totalDefects = 0;
        }

        public void incrementEventCount() {
            this.eventCount++;
        }

        public void addDefects(int defectCount) {
            this.totalDefects += defectCount;
        }

        public String getLineId() {
            return lineId;
        }

        public long getEventCount() {
            return eventCount;
        }

        public long getTotalDefects() {
            return totalDefects;
        }
    }
}