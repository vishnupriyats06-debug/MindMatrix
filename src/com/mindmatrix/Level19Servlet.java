package com.mindmatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Level19Servlet – Level 19: Neural Shift
 * Part 1: Shadow Path (Spatial & Visual Sequential Path Memory with Distraction Flashes)
 * Part 2: Time Echo (Temporal Reasoning & Dynamic Event Sequence Recall)
 */
@WebServlet("/level19")
public class Level19Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String partParam = request.getParameter("part");
        int part = (partParam != null && partParam.equals("2")) ? 2 : 1;

        PrintWriter out = response.getWriter();
        if (part == 1) {
            out.print(generatePart1Data());
        } else {
            out.print(generatePart2Data());
        }
        out.flush();
    }

    /* ═══════════════════════════════════════════════════════════════
       PART 1: SHADOW PATH
       5 Progressive Rounds (3x3 to 5x5) with Path Sequences and Fake Distractions
       ═══════════════════════════════════════════════════════════════ */
    private String generatePart1Data() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"part\":1,");
        sb.append("\"rounds\":[");

        // Round configs: { gridSize, pathLength, fakeFlashesCount, showDurationMs }
        int[][] configs = {
            {3, 3, 0, 1800},
            {3, 4, 1, 1800},
            {4, 5, 2, 2000},
            {4, 6, 3, 2000},
            {5, 7, 4, 2200}
        };

        for (int r = 0; r < configs.length; r++) {
            int size = configs[r][0];
            int pathLen = configs[r][1];
            int fakeCount = configs[r][2];
            int duration = configs[r][3];

            List<Integer> path = generateConnectedPath(size, pathLen);
            Set<Integer> pathSet = new HashSet<>(path);

            List<Integer> availableForFakes = new ArrayList<>();
            int totalCells = size * size;
            for (int c = 0; c < totalCells; c++) {
                if (!pathSet.contains(c)) {
                    availableForFakes.add(c);
                }
            }
            Collections.shuffle(availableForFakes, RAND);
            List<Integer> fakeFlashes = availableForFakes.subList(0, Math.min(fakeCount, availableForFakes.size()));

            if (r > 0) sb.append(",");
            sb.append("{");
            sb.append("\"round\":").append(r + 1).append(",");
            sb.append("\"gridSize\":").append(size).append(",");
            sb.append("\"pathLength\":").append(pathLen).append(",");
            sb.append("\"showDurationMs\":").append(duration).append(",");
            sb.append("\"path\":").append(toJsonArray(path)).append(",");
            sb.append("\"fakeFlashes\":").append(toJsonArray(fakeFlashes));
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private List<Integer> generateConnectedPath(int size, int length) {
        List<Integer> path = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        // Start at random cell
        int current = RAND.nextInt(size * size);
        path.add(current);
        visited.add(current);

        int[][] directions = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, // Orthogonal
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonal
        };

        while (path.size() < length) {
            int r = current / size;
            int c = current % size;

            List<Integer> validNeighbors = new ArrayList<>();
            for (int[] d : directions) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                    int neighborIdx = nr * size + nc;
                    if (!visited.contains(neighborIdx)) {
                        validNeighbors.add(neighborIdx);
                    }
                }
            }

            if (!validNeighbors.isEmpty()) {
                current = validNeighbors.get(RAND.nextInt(validNeighbors.size()));
                path.add(current);
                visited.add(current);
            } else {
                // Backtrack or restart if stuck
                path.clear();
                visited.clear();
                current = RAND.nextInt(size * size);
                path.add(current);
                visited.add(current);
            }
        }

        return path;
    }

    /* ═══════════════════════════════════════════════════════════════
       PART 2: TIME ECHO
       5 Progressive Temporal Reasoning Rounds
       ═══════════════════════════════════════════════════════════════ */
    private static class VisualEvent {
        String id;
        String name;
        String icon;
        String desc;
        String visualClass;
        String color;

        VisualEvent(String id, String name, String icon, String desc, String visualClass, String color) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.desc = desc;
            this.visualClass = visualClass;
            this.color = color;
        }
    }

    private static final VisualEvent[] ALL_EVENTS = {
        new VisualEvent("appear", "Orb Appears in Center", "🟣", "A luminous glowing orb materializes in the center", "anim-appear", "#8b5cf6"),
        new VisualEvent("move_left", "Glides to Left", "⬅️", "The orb smoothly glides to the left side", "anim-move-left", "#06b6d4"),
        new VisualEvent("move_right", "Glides to Right", "➡️", "The orb smoothly glides to the right side", "anim-move-right", "#06b6d4"),
        new VisualEvent("move_up", "Ascends Upward", "⬆️", "The orb rises toward the upper area", "anim-move-up", "#38bdf8"),
        new VisualEvent("move_down", "Descends Downward", "⬇️", "The orb plunges toward the bottom", "anim-move-down", "#38bdf8"),
        new VisualEvent("color_cyan", "Shifts to Neon Cyan", "💎", "The orb's energy shifts to bright neon cyan", "anim-color-cyan", "#00f5ff"),
        new VisualEvent("color_purple", "Shifts to Quantum Violet", "🔮", "The core flashes into deep quantum violet", "anim-color-purple", "#a855f7"),
        new VisualEvent("color_amber", "Shifts to Solar Amber", "⭐", "The aura transforms into intense solar amber", "anim-color-amber", "#f59e0b"),
        new VisualEvent("color_emerald", "Shifts to Emerald Green", "🟢", "A pulse turns the orb into vibrant emerald", "anim-color-emerald", "#10b981"),
        new VisualEvent("color_crimson", "Shifts to Plasma Crimson", "🔴", "The aura ignites into glowing plasma crimson", "anim-color-crimson", "#ef4444"),
        new VisualEvent("shrink", "Contracts to Mini Particle", "🔬", "The entity compresses into a compact particle", "anim-shrink", "#cbd5e1"),
        new VisualEvent("enlarge", "Expands to Giant Core", "🔆", "The entity expands into a large pulsing core", "anim-enlarge", "#fbbf24"),
        new VisualEvent("pulse", "Emits Energy Shockwave", "⚡", "A concentric shockwave radiates outward", "anim-pulse", "#f43f5e"),
        new VisualEvent("rotate", "Spins 360° Clockwise", "🔄", "The core rapidly spins in full rotation", "anim-rotate", "#6366f1"),
        new VisualEvent("split", "Splits into Dual Orbiters", "👥", "The shape divides into dual orbiting spheres", "anim-split", "#ec4899"),
        new VisualEvent("vibrate", "Oscillates at High Frequency", "〰️", "The particle oscillates violently in place", "anim-vibrate", "#eab308"),
        new VisualEvent("disappear", "Dematerializes & Fades Out", "✨", "The particle completely dematerializes and vanishes", "anim-disappear", "#94a3b8")
    };

    private String generatePart2Data() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"part\":2,");
        sb.append("\"rounds\":[");

        int[] eventCounts = {3, 4, 5, 6, 7};

        for (int r = 0; r < eventCounts.length; r++) {
            int count = eventCounts[r];
            List<VisualEvent> sequence = generateEventSequence(count);

            // Generate a temporal reasoning question
            TemporalQuestion q = generateTemporalQuestion(sequence);

            if (r > 0) sb.append(",");
            sb.append("{");
            sb.append("\"round\":").append(r + 1).append(",");
            sb.append("\"eventCount\":").append(count).append(",");
            sb.append("\"events\":[");
            for (int e = 0; e < sequence.size(); e++) {
                VisualEvent ev = sequence.get(e);
                if (e > 0) sb.append(",");
                sb.append("{");
                sb.append("\"id\":\"").append(ev.id).append("\",");
                sb.append("\"name\":\"").append(escapeJson(ev.name)).append("\",");
                sb.append("\"icon\":\"").append(escapeJson(ev.icon)).append("\",");
                sb.append("\"desc\":\"").append(escapeJson(ev.desc)).append("\",");
                sb.append("\"visualClass\":\"").append(ev.visualClass).append("\",");
                sb.append("\"color\":\"").append(ev.color).append("\"");
                sb.append("}");
            }
            sb.append("],");
            sb.append("\"question\":\"").append(escapeJson(q.questionText)).append("\",");
            sb.append("\"correctAnswer\":\"").append(escapeJson(q.correctAnswer)).append("\",");
            sb.append("\"correctIdx\":").append(q.correctChoiceIdx).append(",");
            sb.append("\"choices\":[");
            for (int c = 0; c < q.choices.size(); c++) {
                if (c > 0) sb.append(",");
                sb.append("\"").append(escapeJson(q.choices.get(c))).append("\"");
            }
            sb.append("]");
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private List<VisualEvent> generateEventSequence(int count) {
        List<VisualEvent> seq = new ArrayList<>();
        // First is always appear
        seq.add(ALL_EVENTS[0]);

        // Middle events: pick distinct interesting events
        List<VisualEvent> pool = new ArrayList<>();
        for (int i = 1; i < ALL_EVENTS.length - 1; i++) {
            pool.add(ALL_EVENTS[i]);
        }
        Collections.shuffle(pool, RAND);

        for (int i = 0; i < count - 2; i++) {
            seq.add(pool.get(i));
        }

        // Final event: either disappear or split/vibrate
        if (RAND.nextBoolean()) {
            seq.add(ALL_EVENTS[ALL_EVENTS.length - 1]); // disappear
        } else {
            seq.add(pool.get(count - 2));
        }

        return seq;
    }

    private static class TemporalQuestion {
        String questionText;
        String correctAnswer;
        int correctChoiceIdx;
        List<String> choices;
    }

    private TemporalQuestion generateTemporalQuestion(List<VisualEvent> seq) {
        TemporalQuestion tq = new TemporalQuestion();
        int n = seq.size();
        int qType = RAND.nextInt(4);

        int targetIdx = 0;
        String qText = "";
        String ans = "";

        if (qType == 0 && n >= 2) {
            // "What happened immediately before [Event X]?"
            targetIdx = 1 + RAND.nextInt(n - 1);
            VisualEvent ev = seq.get(targetIdx);
            VisualEvent prevEv = seq.get(targetIdx - 1);
            qText = "What happened immediately before \"" + ev.name + "\"?";
            ans = prevEv.name;
        } else if (qType == 1 && n >= 2) {
            // "What happened immediately after [Event X]?"
            targetIdx = RAND.nextInt(n - 1);
            VisualEvent ev = seq.get(targetIdx);
            VisualEvent nextEv = seq.get(targetIdx + 1);
            qText = "What happened immediately after \"" + ev.name + "\"?";
            ans = nextEv.name;
        } else if (qType == 2 && n >= 3) {
            // "What happened 2 steps before [Event X]?"
            targetIdx = 2 + RAND.nextInt(n - 2);
            VisualEvent ev = seq.get(targetIdx);
            VisualEvent prev2Ev = seq.get(targetIdx - 2);
            qText = "What happened 2 steps before \"" + ev.name + "\"?";
            ans = prev2Ev.name;
        } else {
            // "What was the N-th event in the sequence?"
            targetIdx = RAND.nextInt(n);
            String ord = getOrdinal(targetIdx + 1);
            qText = "What was the " + ord + " event in the sequence?";
            ans = seq.get(targetIdx).name;
        }

        tq.questionText = qText;
        tq.correctAnswer = ans;

        // Distractors: other events from the full pool not equal to answer
        List<String> choices = new ArrayList<>();
        choices.add(ans);

        List<String> otherOptions = new ArrayList<>();
        for (VisualEvent ev : ALL_EVENTS) {
            if (!ev.name.equals(ans) && !otherOptions.contains(ev.name)) {
                otherOptions.add(ev.name);
            }
        }
        Collections.shuffle(otherOptions, RAND);

        for (int i = 0; i < 3 && i < otherOptions.size(); i++) {
            choices.add(otherOptions.get(i));
        }

        Collections.shuffle(choices, RAND);
        tq.choices = choices;
        tq.correctChoiceIdx = choices.indexOf(ans);

        return tq;
    }

    private String getOrdinal(int i) {
        switch (i) {
            case 1: return "1st";
            case 2: return "2nd";
            case 3: return "3rd";
            case 4: return "4th";
            case 5: return "5th";
            case 6: return "6th";
            case 7: return "7th";
            default: return i + "th";
        }
    }

    private String toJsonArray(List<Integer> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
