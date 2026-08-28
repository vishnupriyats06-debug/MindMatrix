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
 * Level18Servlet – Advanced Memory Master Challenge
 * Part 1: Multi-Layer Relationship Memory (6x6 Grid, Symbols+Numbers+Positions, Multi-step recall)
 * Part 2: Memory Transformation Challenge (Mixed sequence + sequential transformation rules)
 */
@WebServlet("/level18")
public class Level18Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String[] SYMBOLS_POOL = {"🔴", "⭐", "🔵", "🟢", "🟡", "◆", "❤️", "🔶", "🟣", "🔺"};

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

    private String generatePart1Data() {
        Random rand = new Random();
        int gridSize = 6;
        int itemCount = 8;

        List<String> symbols = new ArrayList<>(Arrays.asList(SYMBOLS_POOL));
        Collections.shuffle(symbols, rand);

        List<int[]> allCells = new ArrayList<>();
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                allCells.add(new int[]{r, c});
            }
        }
        Collections.shuffle(allCells, rand);

        // Generate items with unique numbers 1-9
        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= 9; i++) nums.add(i);
        Collections.shuffle(nums, rand);

        StringBuilder itemsJson = new StringBuilder("[");
        List<Map<String, Object>> placedItems = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            String sym = symbols.get(i);
            int num = nums.get(i % nums.size());
            int[] pos = allCells.get(i);

            Map<String, Object> item = new HashMap<>();
            item.put("symbol", sym);
            item.put("num", num);
            item.put("r", pos[0]);
            item.put("c", pos[1]);
            placedItems.add(item);

            if (i > 0) itemsJson.append(",");
            itemsJson.append("{\"symbol\":\"").append(sym)
                     .append("\",\"num\":").append(num)
                     .append(",\"r\":").append(pos[0])
                     .append(",\"c\":").append(pos[1]).append("}");
        }
        itemsJson.append("]");

        // Distraction task (Odd shape)
        String distPrompt = "Identify the odd shape out of the group:";
        String distChoices = "[\"○\",\"○\",\"○\",\"△\",\"○\",\"○\"]";
        int distCorrectIdx = 3;

        // Generate 5 Multi-Layer Questions
        // Q1: Direct Recall (What number was associated with Symbol X?)
        Map<String, Object> it1 = placedItems.get(0);
        String q1Prompt = "What number was associated with " + it1.get("symbol") + "?";
        int q1Ans = (int) it1.get("num");
        List<String> q1Opts = generateNumOptions(q1Ans, rand);

        // Q2: Position Recall (Where was Symbol Y located?)
        Map<String, Object> it2 = placedItems.get(1);
        String q2Prompt = "Where was " + it2.get("symbol") + " located in the grid?";
        String q2Ans = "Row " + ((int)it2.get("r") + 1) + ", Col " + ((int)it2.get("c") + 1);
        List<String> q2Opts = generatePosOptions((int)it2.get("r") + 1, (int)it2.get("c") + 1, gridSize, rand);

        // Q3: Reverse Recall (Which symbol was associated with number Z?)
        Map<String, Object> it3 = placedItems.get(2);
        String q3Prompt = "Which symbol was associated with number " + it3.get("num") + "?";
        String q3Ans = (String) it3.get("symbol");
        List<String> q3Opts = generateSymOptions(q3Ans, symbols, rand);

        // Q4: Position-to-Symbol Recall
        Map<String, Object> it4 = placedItems.get(3);
        String q4Prompt = "Which symbol was located at Row " + ((int)it4.get("r") + 1) + ", Column " + ((int)it4.get("c") + 1) + "?";
        String q4Ans = (String) it4.get("symbol");
        List<String> q4Opts = generateSymOptions(q4Ans, symbols, rand);

        // Q5: Advanced Multi-Step Recall
        Map<String, Object> it5 = placedItems.get(4);
        String q5Prompt = "What was the number associated with the symbol " + it5.get("symbol") + "?";
        int q5Ans = (int) it5.get("num");
        List<String> q5Opts = generateNumOptions(q5Ans, rand);

        StringBuilder qJson = new StringBuilder("[");
        qJson.append(buildQJson(q1Prompt, String.valueOf(q1Ans), q1Opts)).append(",");
        qJson.append(buildQJson(q2Prompt, q2Ans, q2Opts)).append(",");
        qJson.append(buildQJson(q3Prompt, q3Ans, q3Opts)).append(",");
        qJson.append(buildQJson(q4Prompt, q4Ans, q4Opts)).append(",");
        qJson.append(buildQJson(q5Prompt, String.valueOf(q5Ans), q5Opts));
        qJson.append("]");

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"part\":1,");
        sb.append("\"gridSize\":").append(gridSize).append(",");
        sb.append("\"memorizeTime\":15,");
        sb.append("\"items\":").append(itemsJson).append(",");
        sb.append("\"distraction\":{");
        sb.append("\"prompt\":\"").append(distPrompt).append("\",");
        sb.append("\"choices\":").append(distChoices).append(",");
        sb.append("\"correctIdx\":").append(distCorrectIdx);
        sb.append("},");
        sb.append("\"questions\":").append(qJson);
        sb.append("}");

        return sb.toString();
    }

    private String buildQJson(String prompt, String answer, List<String> options) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"prompt\":\"").append(prompt).append("\",");
        sb.append("\"answer\":\"").append(answer).append("\",");
        sb.append("\"options\":[");
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(options.get(i)).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    private List<String> generateNumOptions(int correct, Random rand) {
        Set<String> opts = new LinkedHashSet<>();
        opts.add(String.valueOf(correct));
        while (opts.size() < 4) {
            opts.add(String.valueOf(1 + rand.nextInt(9)));
        }
        List<String> list = new ArrayList<>(opts);
        Collections.shuffle(list, rand);
        return list;
    }

    private List<String> generatePosOptions(int r, int c, int maxGrid, Random rand) {
        Set<String> opts = new LinkedHashSet<>();
        opts.add("Row " + r + ", Col " + c);
        while (opts.size() < 4) {
            int randR = 1 + rand.nextInt(maxGrid);
            int randC = 1 + rand.nextInt(maxGrid);
            opts.add("Row " + randR + ", Col " + randC);
        }
        List<String> list = new ArrayList<>(opts);
        Collections.shuffle(list, rand);
        return list;
    }

    private List<String> generateSymOptions(String correct, List<String> pool, Random rand) {
        Set<String> opts = new LinkedHashSet<>();
        opts.add(correct);
        while (opts.size() < 4) {
            opts.add(pool.get(rand.nextInt(pool.size())));
        }
        List<String> list = new ArrayList<>(opts);
        Collections.shuffle(list, rand);
        return list;
    }

    private String generatePart2Data() {
        // Sequence: 8 elements
        // e.g. A, 7, 🔺, 3, B, ⭐, 9, C
        String[] origSeq = {"A", "7", "🔺", "3", "B", "⭐", "9", "C"};
        // Transformation:
        // Rule 1: Divide into pairs: (A, 7), (🔺, 3), (B, ⭐), (9, C)
        // Rule 2: Reverse each pair: (7, A), (3, 🔺), (⭐, B), (C, 9)
        // Rule 3: Move last pair to start: (C, 9), (7, A), (3, 🔺), (⭐, B)
        // Rule 4: Reverse complete sequence: B, ⭐, 🔺, 3, A, 7, 9, C
        String[] finalSeq = {"B", "⭐", "🔺", "3", "A", "7", "9", "C"};

        String[] rules = {
            "RULE 1: Divide the sequence into pairs.",
            "RULE 2: Reverse the elements inside every pair.",
            "RULE 3: Move the last pair to the beginning.",
            "RULE 4: Reverse the complete resulting sequence."
        };

        StringBuilder origJson = new StringBuilder("[");
        for (int i = 0; i < origSeq.length; i++) {
            if (i > 0) origJson.append(",");
            origJson.append("\"").append(origSeq[i]).append("\"");
        }
        origJson.append("]");

        StringBuilder finalJson = new StringBuilder("[");
        for (int i = 0; i < finalSeq.length; i++) {
            if (i > 0) finalJson.append(",");
            finalJson.append("\"").append(finalSeq[i]).append("\"");
        }
        finalJson.append("]");

        StringBuilder rulesJson = new StringBuilder("[");
        for (int i = 0; i < rules.length; i++) {
            if (i > 0) rulesJson.append(",");
            rulesJson.append("\"").append(rules[i]).append("\"");
        }
        rulesJson.append("]");

        // Palette tokens (all unique elements + 4 distractor tokens)
        String[] palette = {"A", "B", "C", "7", "3", "9", "🔺", "⭐", "K", "5", "🔵", "◆"};

        StringBuilder palJson = new StringBuilder("[");
        for (int i = 0; i < palette.length; i++) {
            if (i > 0) palJson.append(",");
            palJson.append("\"").append(palette[i]).append("\"");
        }
        palJson.append("]");

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"part\":2,");
        sb.append("\"memorizeTime\":15,");
        sb.append("\"originalSequence\":").append(origJson).append(",");
        sb.append("\"rules\":").append(rulesJson).append(",");
        sb.append("\"expectedSequence\":").append(finalJson).append(",");
        sb.append("\"palette\":").append(palJson);
        sb.append("}");

        return sb.toString();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"ok\"}");
    }
}
