package com.mindmatrix;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * StreakDAO – Data Access Object for Persistent Daily Activity History & Calendar-Day Streak Calculation.
 * Maintains streak strictly based on local calendar dates (YYYY-MM-DD).
 */
public class StreakDAO {

    public static class StreakInfo {
        public int currentStreak;
        public int longestStreak;
        public LocalDate lastActivityDate;
        public boolean playedToday;
        public List<String> activityDates;

        public StreakInfo(int currentStreak, int longestStreak, LocalDate lastActivityDate, boolean playedToday, List<String> activityDates) {
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
            this.lastActivityDate = lastActivityDate;
            this.playedToday = playedToday;
            this.activityDates = (activityDates != null) ? activityDates : new ArrayList<>();
        }
    }

    public static LocalDate getToday() {
        return LocalDate.now();
    }

    public static LocalDate getTodayKolkata() {
        return getToday();
    }

    /**
     * Fetches all recorded activity dates for a user, sorted chronologically.
     */
    public static List<LocalDate> getActivityDates(Connection conn, int userId) throws SQLException {
        List<LocalDate> dates = new ArrayList<>();
        String sql = "SELECT activity_date FROM user_activity_dates WHERE user_id = ? ORDER BY activity_date ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("activity_date");
                    if (d != null) {
                        dates.add(d.toLocalDate());
                    }
                }
            }
        }
        return dates;
    }

    /**
     * Calculates current consecutive streak and longest historical streak from activity dates
     * based purely on calendar dates (YYYY-MM-DD). Ignores any future dates beyond today.
     */
    public static StreakInfo computeStreak(Connection conn, int userId, LocalDate today) throws SQLException {
        LocalDate actualToday = getToday();
        if (today == null || today.isAfter(actualToday)) {
            today = actualToday;
        }
        List<LocalDate> rawDates = getActivityDates(conn, userId);
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d : rawDates) {
            if (!d.isAfter(today)) {
                dates.add(d);
            }
        }
        LocalDate yesterday = today.minusDays(1);

        Set<LocalDate> dateSet = new HashSet<>(dates);
        boolean playedToday = dateSet.contains(today);

        // 1. Compute current consecutive streak based on calendar days
        int currentStreak = 0;
        if (playedToday) {
            LocalDate check = today;
            while (dateSet.contains(check)) {
                currentStreak++;
                check = check.minusDays(1);
            }
        } else if (dateSet.contains(yesterday)) {
            LocalDate check = yesterday;
            while (dateSet.contains(check)) {
                currentStreak++;
                check = check.minusDays(1);
            }
        }

        // 2. Compute longest streak in history
        int longestStreak = 0;
        if (!dates.isEmpty()) {
            int tempStreak = 1;
            longestStreak = 1;
            for (int i = 1; i < dates.size(); i++) {
                if (dates.get(i).equals(dates.get(i - 1).plusDays(1))) {
                    tempStreak++;
                    if (tempStreak > longestStreak) {
                        longestStreak = tempStreak;
                    }
                } else if (!dates.get(i).equals(dates.get(i - 1))) {
                    tempStreak = 1;
                }
            }
        }
        longestStreak = Math.max(longestStreak, currentStreak);

        // Check best_streak from user_progress to never downgrade historical records
        String sql = "SELECT best_streak, last_played_date FROM user_progress WHERE user_id = ?";
        LocalDate dbLastDate = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int dbBest = rs.getInt("best_streak");
                    longestStreak = Math.max(longestStreak, dbBest);
                    Date ld = rs.getDate("last_played_date");
                    if (ld != null) {
                        LocalDate parsedLd = ld.toLocalDate();
                        if (!parsedLd.isAfter(today)) {
                            dbLastDate = parsedLd;
                        }
                    }
                }
            }
        }

        List<String> dateStrings = new ArrayList<>();
        for (LocalDate d : dates) {
            dateStrings.add(d.toString());
        }

        LocalDate lastActivity = dates.isEmpty() ? dbLastDate : dates.get(dates.size() - 1);

        return new StreakInfo(currentStreak, longestStreak, lastActivity, playedToday, dateStrings);
    }

    public static StreakInfo computeStreak(Connection conn, int userId) throws SQLException {
        return computeStreak(conn, userId, getToday());
    }

    /**
     * Records a level completion for the user on activityDate (user's local calendar date).
     * Multiple completions on the same calendar day result in ONLY ONE record in user_activity_dates.
     * Future dates beyond today are strictly prevented.
     */
    public static StreakInfo recordLevelCompletion(Connection conn, int userId, LocalDate activityDate) throws SQLException {
        LocalDate actualToday = getToday();
        if (activityDate == null || activityDate.isAfter(actualToday)) {
            activityDate = actualToday;
        }

        // 1. Insert activityDate into user_activity_dates (unique constraint ignores duplicates)
        String insSql = "INSERT IGNORE INTO user_activity_dates (user_id, activity_date) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insSql)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(activityDate));
            stmt.executeUpdate();
        }

        // 2. Compute accurate streak and best streak
        StreakInfo updated = computeStreak(conn, userId, activityDate);

        // 3. Update user_progress table
        String upSql = "UPDATE user_progress SET streak = ?, best_streak = ?, last_played_date = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(upSql)) {
            stmt.setInt(1, updated.currentStreak);
            stmt.setInt(2, updated.longestStreak);
            stmt.setDate(3, Date.valueOf(activityDate));
            stmt.setInt(4, userId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                String insProgSql = "INSERT INTO user_progress (user_id, streak, best_streak, last_played_date, hints) VALUES (?, ?, ?, ?, 3)";
                try (PreparedStatement insProg = conn.prepareStatement(insProgSql)) {
                    insProg.setInt(1, userId);
                    insProg.setInt(2, updated.currentStreak);
                    insProg.setInt(3, updated.longestStreak);
                    insProg.setDate(4, Date.valueOf(activityDate));
                    insProg.executeUpdate();
                }
            }
        }

        return updated;
    }

    public static StreakInfo recordLevelCompletion(Connection conn, int userId) throws SQLException {
        return recordLevelCompletion(conn, userId, getToday());
    }
}
