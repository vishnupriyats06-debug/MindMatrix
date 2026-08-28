package com.mindmatrix;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * StreakUtil - Centralized helper for calendar-day based streak calculation.
 * Computes streak based purely on calendar dates (YYYY-MM-DD) without time-of-day restrictions.
 */
public class StreakUtil {

    public static int calculateDayStreak(int currentStreak, Date lastPlayedDate, boolean isSaving, LocalDate today) {
        if (today == null) {
            today = LocalDate.now();
        }
        if (lastPlayedDate == null) {
            return isSaving ? 1 : 0;
        }
        LocalDate lastPlayed = lastPlayedDate.toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(lastPlayed, today);

        if (daysBetween == 0) {
            // Played today already: streak stays same on multiple plays today
            return currentStreak > 0 ? currentStreak : (isSaving ? 1 : 0);
        } else if (daysBetween == 1) {
            // Played yesterday: increment if saving today, keep current if loading
            return isSaving ? (currentStreak + 1) : currentStreak;
        } else {
            // Missed 1 or more full days: reset streak to 1 if saving today
            return isSaving ? 1 : currentStreak;
        }
    }

    public static int calculateDayStreak(int currentStreak, Date lastPlayedDate, boolean isSaving) {
        return calculateDayStreak(currentStreak, lastPlayedDate, isSaving, LocalDate.now());
    }
}
