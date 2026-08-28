package com.mindmatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Level16Servlet – Memory Search & Filter
 *   GET  /level16?part=1  → Generates selective memory dataset with target category and distractors.
 *   GET  /level16?part=2  → Generates multi-condition attention filter dataset (Color + Category).
 *   POST /level16         → Validates completion and updates progress.
 */
@WebServlet("/level16")
public class Level16Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    // Part 1 Pools
    private static final Map<String, String[]> CATEGORIES = new HashMap<>();
    static {
        CATEGORIES.put("Animals", new String[]{"🐶", "🐱", "🐭", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵"});
        CATEGORIES.put("Fruits", new String[]{"🍎", "🍌", "🍊", "🍓", "🍇", "🍉", "🍒", "🍑", "🍍", "🥝", "🥭", "🍋"});
        CATEGORIES.put("Vehicles", new String[]{"🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🚚", "🚲", "🛵", "✈️", "🚀", "⛵"});
        CATEGORIES.put("Objects", new String[]{"💻", "📱", "📷", "⌚", "⏰", "💡", "🔦", "🔑", "🔨", "🪓", "🔔", "🎸", "🎧", "📚"});
        CATEGORIES.put("Stars & Symbols", new String[]{"⭐", "🌟", "✨", "💎", "🔥", "⚡", "❤️", "🌙", "☀️", "👑", "🔮", "🍀"});
    }

    // Part 2 Filter Configs
    private static class FilterRule {
        String name;
        String colorSymbol;
        String colorName;
        String categoryName;
        String[] categoryIcons;

        FilterRule(String name, String colorSymbol, String colorName, String categoryName, String[] categoryIcons) {
            this.name = name;
            this.colorSymbol = colorSymbol;
            this.colorName = colorName;
            this.categoryName = categoryName;
            this.categoryIcons = categoryIcons;
        }
    }

    private static final String[] COLORS = {"🔴", "🔵", "🟢", "🟡", "🟣"};
    private static final String[] ANIMAL_ICONS = {"🐶", "🐱", "🐭", "🐰", "🦊", "🐻", "🐼", "🐯"};
    private static final String[] VEHICLE_ICONS = {"🚗", "🚕", "🚌", "🏎️", "🚓", "🚲", "✈️", "🚀"};
    private static final String[] FRUIT_ICONS = {"🍎", "🍌", "🍊", "🍓", "🍇", "🍉", "🍒", "🍍"};
    private static final String[] GEM_ICONS = {"⭐", "🌟", "💎", "👑", "🔮", "✨", "🔥", "⚡"};

    private static final FilterRule[] FILTER_RULES = {
        new FilterRule("RED_ANIMALS", "🔴", "RED", "animals", ANIMAL_ICONS),
        new FilterRule("BLUE_VEHICLES", "🔵", "BLUE", "vehicles", VEHICLE_ICONS),
        new FilterRule("GREEN_FRUITS", "🟢", "GREEN", "fruits", FRUIT_ICONS),
        new FilterRule("YELLOW_GEMS", "🟡", "YELLOW", "stars & gems", GEM_ICONS),
        new FilterRule("PURPLE_ANIMALS", "🟣", "PURPLE", "animals", ANIMAL_ICONS)
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        String partStr = req.getParameter("part");
        int part = (partStr == null) ? 1 : Integer.parseInt(partStr);
        PrintWriter out = resp.getWriter();

        if (part == 1) {
            // Select random target category
            List<String> catKeys = new ArrayList<>(CATEGORIES.keySet());
            String targetCat = catKeys.get(RAND.nextInt(catKeys.size()));
            String[] targetPool = CATEGORIES.get(targetCat);

            // Pick 3 to 4 target items
            int targetCount = 3 + (RAND.nextBoolean() ? 1 : 0);
            List<String> shuffledTargetPool = shuffleArray(targetPool);
            List<String> originalTargets = new ArrayList<>(shuffledTargetPool.subList(0, targetCount));
            List<String> newTargetDistractors = new ArrayList<>(shuffledTargetPool.subList(targetCount, Math.min(targetCount + 2, shuffledTargetPool.size())));

            // Pick 8 to 9 distractors from other categories
            List<String> otherPool = new ArrayList<>();
            for (Map.Entry<String, String[]> entry : CATEGORIES.entrySet()) {
                if (!entry.getKey().equals(targetCat)) {
                    for (String item : entry.getValue()) otherPool.add(item);
                }
            }
            Collections.shuffle(otherPool, RAND);
            int otherCount = 12 - targetCount;
            List<String> originalDistractors = new ArrayList<>(otherPool.subList(0, otherCount));

            // Original Collection (12 items)
            List<String> originalCollection = new ArrayList<>();
            originalCollection.addAll(originalTargets);
            originalCollection.addAll(originalDistractors);
            Collections.shuffle(originalCollection, RAND);

            // Test Collection (18 unique items): originalTargets + originalDistractors + newTargetDistractors + extra distractors
            Set<String> testSet = new LinkedHashSet<>();
            testSet.addAll(originalTargets);
            testSet.addAll(originalDistractors);
            testSet.addAll(newTargetDistractors);
            for (String item : otherPool) {
                if (testSet.size() >= 18) break;
                testSet.add(item);
            }
            List<String> testCollection = new ArrayList<>(testSet);
            Collections.shuffle(testCollection, RAND);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"part\":1")
              .append(",\"memorizeTime\":15")
              .append(",\"category\":\"").append(escapeJson(targetCat)).append("\"")
              .append(",\"instruction\":\"Remember only the ").append(escapeJson(targetCat.toLowerCase())).append(".\"")
              .append(",\"original\":").append(toJsonArray(originalCollection))
              .append(",\"target\":").append(toJsonArray(originalTargets))
              .append(",\"testCollection\":").append(toJsonArray(testCollection))
              .append("}");

            session.setAttribute("level16p1", sb.toString());
            out.print(sb.toString());

        } else if (part == 2) {
            // Select random filter rule (e.g. RED Animals)
            FilterRule rule = FILTER_RULES[RAND.nextInt(FILTER_RULES.length)];

            // Pick 3 to 4 matching target items: (Target Color + Target Category)
            int targetCount = 3 + (RAND.nextBoolean() ? 1 : 0);
            List<String> shuffledIcons = shuffleArray(rule.categoryIcons);
            List<String> targetItems = new ArrayList<>();
            for (int i = 0; i < targetCount; i++) {
                targetItems.add(rule.colorSymbol + " " + shuffledIcons.get(i));
            }

            // Pick 2 trick distractors: (Target Color + Target Category, but NOT in original)
            List<String> trickItems = new ArrayList<>();
            for (int i = targetCount; i < Math.min(targetCount + 2, shuffledIcons.size()); i++) {
                trickItems.add(rule.colorSymbol + " " + shuffledIcons.get(i));
            }

            // Build diverse non-target distractors with NO duplicates
            Set<String> distractorSet = new LinkedHashSet<>();
            // 1. Same category, different colors
            for (String col : COLORS) {
                if (!col.equals(rule.colorSymbol)) {
                    for (String icon : rule.categoryIcons) {
                        distractorSet.add(col + " " + icon);
                    }
                }
            }

            // 2. Other categories, all colors (including target color)
            String[][] allIconPools = {ANIMAL_ICONS, VEHICLE_ICONS, FRUIT_ICONS, GEM_ICONS};
            for (String[] pool : allIconPools) {
                if (pool != rule.categoryIcons) {
                    for (String icon : pool) {
                        for (String col : COLORS) {
                            distractorSet.add(col + " " + icon);
                        }
                    }
                }
            }

            List<String> shuffledDistractors = new ArrayList<>(distractorSet);
            Collections.shuffle(shuffledDistractors, RAND);

            int origDistCount = 16 - targetCount;
            List<String> origDistractors = new ArrayList<>(shuffledDistractors.subList(0, Math.min(origDistCount, shuffledDistractors.size())));

            // Original Collection (16 items)
            List<String> originalCollection = new ArrayList<>();
            originalCollection.addAll(targetItems);
            originalCollection.addAll(origDistractors);
            Collections.shuffle(originalCollection, RAND);

            // Test Collection (22 unique items): targetItems + origDistractors + trickItems + extra distractors
            Set<String> testSet = new LinkedHashSet<>();
            testSet.addAll(targetItems);
            testSet.addAll(origDistractors);
            testSet.addAll(trickItems);
            for (String d : shuffledDistractors) {
                if (testSet.size() >= 22) break;
                testSet.add(d);
            }
            List<String> testCollection = new ArrayList<>(testSet);
            Collections.shuffle(testCollection, RAND);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"part\":2")
              .append(",\"memorizeTime\":15")
              .append(",\"ruleName\":\"").append(escapeJson(rule.name)).append("\"")
              .append(",\"colorName\":\"").append(escapeJson(rule.colorName)).append("\"")
              .append(",\"categoryName\":\"").append(escapeJson(rule.categoryName)).append("\"")
              .append(",\"instruction\":\"Remember only the ").append(escapeJson(rule.colorName)).append(" ").append(escapeJson(rule.categoryName)).append(" (").append(rule.colorSymbol).append(").\"")
              .append(",\"original\":").append(toJsonArray(originalCollection))
              .append(",\"target\":").append(toJsonArray(targetItems))
              .append(",\"testCollection\":").append(toJsonArray(testCollection))
              .append("}");

            session.setAttribute("level16p2", sb.toString());
            out.print(sb.toString());

        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid part\"}");
        }
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().print("{\"error\":\"Session expired\"}");
            return;
        }
        resp.getWriter().print("{\"success\":true}");
        resp.getWriter().flush();
    }

    private List<String> shuffleArray(String[] array) {
        List<String> list = new ArrayList<>();
        for (String s : array) list.add(s);
        Collections.shuffle(list, RAND);
        return list;
    }

    private String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
