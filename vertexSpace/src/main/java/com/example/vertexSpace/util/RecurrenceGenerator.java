package com.example.vertexSpace.util;

import com.example.vertexSpace.dto.request.CreateRecurringBookingRequest;
import com.example.vertexSpace.enums.RecurrenceEndType;
import com.example.vertexSpace.enums.RecurrencePattern;
import com.example.vertexSpace.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for generating recurring booking occurrences
 *
 * Converts user's timezone-aware requests into a list of occurrence dates
 * Example: "Every Monday 2-4 PM IST for 12 weeks" → [Mar 3, Mar 10, Mar 17, ...]
 */
@Component
@Slf4j
public class RecurrenceGenerator {

    private static final int MAX_OCCURRENCES = 52;  // 1 year max
    private static final int MAX_SEARCH_DAYS = 730; // 2 years max search range

    /**
     * Generate all occurrence dates based on recurrence pattern
     *
     * @param tz The recurring booking request
     * @return List of occurrence timestamps (in original timezone)
     */
    private ZoneId parseTimezone(String tz) {
        if (tz == null || tz.isBlank() || "string".equalsIgnoreCase(tz.trim())) {
            throw new ValidationException("timezone is required (e.g., 'UTC', 'Asia/Kolkata')");
        }
        String raw = tz.trim();

        // quick common aliases
        if (raw.equalsIgnoreCase("Z") || raw.equalsIgnoreCase("UTC")) return ZoneOffset.UTC;

        try {
            return ZoneId.of(raw); // exact match first
        } catch (DateTimeException ignored) {
            // case-insensitive match against available IDs (e.g., "Utc" -> "UTC")
            for (String id : ZoneId.getAvailableZoneIds()) {
                if (id.equalsIgnoreCase(raw)) return ZoneId.of(id);
            }
            throw new ValidationException("Invalid timezone: " + tz + " (use IANA ID like 'Asia/Kolkata' or 'UTC')");
        }
    }


    public List<LocalDateTime> generateOccurrences(CreateRecurringBookingRequest request) {
        log.info("Generating occurrences for pattern: {}", request.getPattern());

        ZoneId timezone = parseTimezone(request.getTimezone());
        LocalDateTime firstOccurrence = LocalDateTime.ofInstant(request.getStartTime(), timezone);
        LocalDateTime searchLimit = calculateSearchLimit(request, firstOccurrence, timezone);

        List<LocalDateTime> occurrences;

        switch (request.getPattern()) {
            case WEEKLY -> occurrences = generateWeeklyOccurrences(
                    firstOccurrence, searchLimit, request.getDaysOfWeek(), request.getNumberOfOccurrences()
            );
            case DAILY -> occurrences = generateDailyOccurrences(
                    firstOccurrence, searchLimit, request.getNumberOfOccurrences()
            );
            case MONTHLY -> occurrences = generateMonthlyOccurrences(
                    firstOccurrence, searchLimit, request.getDayOfMonth(), request.getNumberOfOccurrences()
            );
            default -> throw new ValidationException("Unsupported recurrence pattern: " + request.getPattern());
        }

        if (occurrences.isEmpty()) {
            throw new ValidationException("No valid occurrences generated. Check your recurrence settings.");
        }

        log.info("Generated {} occurrences", occurrences.size());
        return occurrences;
    }

    /**
     * Generate weekly occurrences
     * Example: Every Monday and Wednesday for 12 weeks
     */
    private List<LocalDateTime> generateWeeklyOccurrences(
            LocalDateTime start,
            LocalDateTime limit,
            List<DayOfWeek> daysOfWeek,
            Integer maxOccurrences) {

        List<LocalDateTime> occurrences = new ArrayList<>();
        LocalDateTime current = start;
        int daysSearched = 0;

        while (current.isBefore(limit) &&
                (maxOccurrences == null || occurrences.size() < maxOccurrences) &&
                daysSearched < MAX_SEARCH_DAYS) {

            if (daysOfWeek.contains(current.getDayOfWeek())) {
                occurrences.add(current);
            }
            current = current.plusDays(1);
            daysSearched++;
        }

        return occurrences;
    }

    /**
     * Generate daily occurrences
     * Example: Every day for 30 days
     */
    private List<LocalDateTime> generateDailyOccurrences(
            LocalDateTime start,
            LocalDateTime limit,
            Integer maxOccurrences) {

        List<LocalDateTime> occurrences = new ArrayList<>();
        LocalDateTime current = start;
        int count = 0;

        while (current.isBefore(limit) &&
                (maxOccurrences == null || count < maxOccurrences) &&
                count < MAX_SEARCH_DAYS) {

            occurrences.add(current);
            current = current.plusDays(1);
            count++;
        }

        return occurrences;
    }

    /**
     * Generate monthly occurrences
     * Example: 15th of every month for 6 months
     */
    private List<LocalDateTime> generateMonthlyOccurrences(
            LocalDateTime start,
            LocalDateTime limit,
            Integer dayOfMonth,
            Integer maxOccurrences) {

        List<LocalDateTime> occurrences = new ArrayList<>();
        LocalDateTime current = adjustToTargetDay(start, dayOfMonth);
        int count = 0;

        while (current.isBefore(limit) &&
                (maxOccurrences == null || count < maxOccurrences) &&
                count < MAX_OCCURRENCES) {

            occurrences.add(current);
            current = current.plusMonths(1);

            // Handle months with fewer days (e.g., Feb 30 → Feb 28)
            current = adjustToTargetDay(current, dayOfMonth);
            count++;
        }

        return occurrences;
    }

    /**
     * Adjust date to target day of month, handling edge cases
     * Example: dayOfMonth=31 in February → February 28/29
     */
    private LocalDateTime adjustToTargetDay(LocalDateTime dateTime, int dayOfMonth) {
        int maxDayInMonth = dateTime.toLocalDate().lengthOfMonth();
        int targetDay = Math.min(dayOfMonth, maxDayInMonth);
        return dateTime.withDayOfMonth(targetDay);
    }

    /**
     * Calculate search limit based on end condition
     */
    private LocalDateTime calculateSearchLimit(
            CreateRecurringBookingRequest request,
            LocalDateTime start,
            ZoneId timezone) {

        if (request.getEndType() == RecurrenceEndType.END_DATE) {
            return LocalDateTime.ofInstant(request.getRecurrenceEndDate(), timezone);
        }

        // For OCCURRENCES or NEVER: default to 2 years max
        return start.plusYears(2);
    }

    /**
     * Convert LocalDateTime (in user's timezone) to Instant (UTC)
     */
    public Instant toUTC(LocalDateTime localDateTime, String timezone) {
        return localDateTime.atZone(ZoneId.of(timezone)).toInstant();
    }

    /**
     * Convert Instant (UTC) to LocalDateTime (in user's timezone)
     */
    public LocalDateTime toLocalTime(Instant instant, String timezone) {
        return LocalDateTime.ofInstant(instant, ZoneId.of(timezone));
    }
}
