package com.example.MachineEventStore.util;


import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class DateTimeUtil {

    /**
     * Calculates the duration between two instants in hours
     * Used for calculating time windows in statistics
     *
     * @param start the start instant (inclusive)
     * @param end the end instant (exclusive)
     * @return duration in hours as a double
     */
    public double calculateHoursBetween(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times cannot be null");
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }

        Duration duration = Duration.between(start, end);
        long seconds = duration.getSeconds();
        return seconds / 3600.0;
    }

    /**
     * Gets the current instant (server time)
     * Used for setting receivedTime
     *
     * @return current instant in UTC
     */
    public Instant now() {
        return Instant.now();
    }

    /**
     * Checks if an instant is within a time range
     * Start is inclusive, end is exclusive
     *
     * @param instant the instant to check
     * @param start the start of the range (inclusive)
     * @param end the end of the range (exclusive)
     * @return true if instant is in range
     */
    public boolean isInRange(Instant instant, Instant start, Instant end) {
        if (instant == null || start == null || end == null) {
            return false;
        }

        // start <= instant < end
        return !instant.isBefore(start) && instant.isBefore(end);

    }

    /**
     * Validates that start is before end
     *
     * @param start the start instant
     * @param end the end instant
     * @return true if valid (start < end)
     */
    public boolean isValidTimeRange(Instant start, Instant end) {
        if (start == null || end == null) {
            return false;
        }

        return start.isBefore(end);
    }

    /**
     * Rounds a double to specified decimal places
     * Used for rounding defectsPercent to 2 decimal places
     *
     * @param value the value to round
     * @param decimalPlaces number of decimal places
     * @return rounded value
     */
    public double round(double value, int decimalPlaces) {
        double multiplier = Math.pow(10, decimalPlaces);
        return Math.round(value * multiplier) / multiplier;
    }

    /**
     * Calculates defect rate per hour
     * Formula: defectsCount / windowHours
     *
     * @param defectsCount total defects in window
     * @param windowHours duration of window in hours
     * @return defects per hour
     */
    public double calculateDefectRate(long defectsCount, double windowHours) {
        if (windowHours <= 0) {
            return 0.0;
        }

        return defectsCount / windowHours;
    }

    /**
     * Calculates defects percentage per 100 events
     * Formula: (totalDefects / eventCount) * 100
     * Rounded to 2 decimal places
     *
     * @param totalDefects total defects
     * @param eventCount total events
     * @return defects per 100 events, rounded to 2 decimals
     */
    public double calculateDefectsPercent(long totalDefects, long eventCount) {
        if (eventCount == 0) {
            return 0.0;
        }

        double percent = (totalDefects / (double) eventCount) * 100.0;
        return round(percent, 2);
    }
}
