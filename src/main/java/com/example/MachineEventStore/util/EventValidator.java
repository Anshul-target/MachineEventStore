package com.example.MachineEventStore.util;


import com.example.MachineEventStore.exception.ValidationException;
import com.example.MachineEventStore.model.dto.EventRequest;
import com.example.MachineEventStore.model.enums.RejectionReason;
//import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class EventValidator {

    // Constants for validation rules
    private static final long MIN_DURATION_MS = 0L;
    private static final long MAX_DURATION_MS = 21_600_000L; // 6 hours in milliseconds
    private static final long FUTURE_TIME_THRESHOLD_MINUTES = 15L;

    /**
     * Validates an event request according to business rules
     * @param eventRequest the event to validate
     * @throws ValidationException if validation fails
     */
    public void validate(EventRequest eventRequest) {
        validateRequiredFields(eventRequest);
        validateDuration(eventRequest.getDurationMs(), eventRequest.getEventId());
        validateEventTime(eventRequest.getEventTime(), eventRequest.getEventId());
    }

    /**
     * Validates that all required fields are present and not null
     */
    private void validateRequiredFields(EventRequest eventRequest) {
        if (eventRequest.getEventId() == null || eventRequest.getEventId().isBlank()) {
            throw new ValidationException("Event ID is required", RejectionReason.INVALID_PAYLOAD);
        }

        if (eventRequest.getEventTime() == null) {
            throw new ValidationException("Event time is required", RejectionReason.INVALID_PAYLOAD);
        }

        if (eventRequest.getMachineId() == null || eventRequest.getMachineId().isBlank()) {
            throw new ValidationException("Machine ID is required", RejectionReason.INVALID_PAYLOAD);
        }

        if (eventRequest.getDurationMs() == null) {
            throw new ValidationException("Duration is required", RejectionReason.INVALID_PAYLOAD);
        }

        if (eventRequest.getDefectCount() == null) {
            throw new ValidationException("Defect count is required", RejectionReason.INVALID_PAYLOAD);
        }

        if (eventRequest.getLineId() == null || eventRequest.getLineId().isBlank()) {
            throw new ValidationException("Line ID is required", RejectionReason.INVALID_PAYLOAD);
        }

        if (eventRequest.getFactoryId() == null || eventRequest.getFactoryId().isBlank()) {
            throw new ValidationException("Factory ID is required", RejectionReason.INVALID_PAYLOAD);
        }
    }

    /**
     * Validates duration is within acceptable range
     * Rule: 0 <= durationMs <= 6 hours (21,600,000 ms)
     */
    private void validateDuration(Long durationMs, String eventId) {
        if (durationMs < MIN_DURATION_MS || durationMs > MAX_DURATION_MS) {
            throw new ValidationException(
                    String.format("Invalid duration for event %s: %d ms. Must be between %d and %d ms",
                            eventId, durationMs, MIN_DURATION_MS, MAX_DURATION_MS),
                    RejectionReason.INVALID_DURATION

            );
        }
    }

    /**
     * Validates that event time is not too far in the future
     * Rule: eventTime cannot be more than 15 minutes in the future
     */
    private void validateEventTime(Instant eventTime, String eventId) {
        Instant now = Instant.now();
        log.info("NOW (UTC) = {}", Instant.now());

        Instant maxAllowedTime = now.plus(FUTURE_TIME_THRESHOLD_MINUTES, ChronoUnit.MINUTES);

        if (eventTime.isAfter(maxAllowedTime)) {
            throw new ValidationException(
                    String.format("Event time for event %s is too far in the future. Event time: %s, Current time: %s",
                            eventId, eventTime, now),
                    RejectionReason.FUTURE_EVENT_TIME
            );
        }
    }

    /**
     * Checks if defect count represents "unknown" defects
     * Rule: defectCount = -1 means unknown
     */
    public boolean isUnknownDefectCount(Integer defectCount) {
        return defectCount != null && defectCount == -1;
    }

    /**
     * Gets the maximum allowed duration in milliseconds
     */
    public static long getMaxDurationMs() {
        return MAX_DURATION_MS;
    }

    /**
     * Gets the minimum allowed duration in milliseconds
     */
    public static long getMinDurationMs() {
        return MIN_DURATION_MS;
    }

    /**
     * Gets the future time threshold in minutes
     */
    public static long getFutureTimeThresholdMinutes() {
        return FUTURE_TIME_THRESHOLD_MINUTES;
    }
}