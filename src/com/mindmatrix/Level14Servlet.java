package com.mindmatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Level14Servlet – Sequence Transformation Memory
 *   GET  /level14?part=1  → Generates a sequence of numbers and letters with a transformation rule.
 *   GET  /level14?part=2  → Generates a complex mixed sequence (letters, numbers, symbols) with an advanced rule.
 *   POST /level14         → Validates completion and updates progress.
 */
@WebServlet("/level14")
public class Level14Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    private static final String[] LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "M", "N", "P", "R", "T", "W", "X", "Y"};
    private static final String[] NUMBERS = {"2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String[] SYMBOLS = {"★", "▲", "●", "◆", "■", "✦", "⬟", "✿"};

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
            // Part 1: 6 to 8 elements alternating / mixing numbers and letters
            int count = 6 + (RAND.nextBoolean() ? 2 : 0); // 6 or 8 elements
            int half = count / 2;

            List<String> chosenLetters = pickRandom(LETTERS, half);
            List<String> chosenNumbers = pickRandom(NUMBERS, half);

            List<String> sequence = new ArrayList<>();
            for (int i = 0; i < half; i++) {
                if (RAND.nextBoolean()) {
                    sequence.add(chosenLetters.get(i));
                    sequence.add(chosenNumbers.get(i));
                } else {
                    sequence.add(chosenNumbers.get(i));
                    sequence.add(chosenLetters.get(i));
                }
            }

            // Choose a transformation rule
            int ruleType = RAND.nextInt(5);
            String ruleId;
            String ruleText;
            List<String> transformed = new ArrayList<>();

            List<String> seqLetters = new ArrayList<>();
            List<String> seqNumbers = new ArrayList<>();
            for (String s : sequence) {
                if (isLetter(s)) seqLetters.add(s);
                else seqNumbers.add(s);
            }

            switch (ruleType) {
                case 0:
                    ruleId = "LETTERS_FIRST";
                    ruleText = "Enter all alphabets first, followed by all numbers (maintaining original relative order).";
                    transformed.addAll(seqLetters);
                    transformed.addAll(seqNumbers);
                    break;
                case 1:
                    ruleId = "NUMBERS_FIRST";
                    ruleText = "Enter all numbers first, followed by all alphabets (maintaining original relative order).";
                    transformed.addAll(seqNumbers);
                    transformed.addAll(seqLetters);
                    break;
                case 2:
                    ruleId = "LETTERS_NUMBERS_REVERSE";
                    ruleText = "Enter all alphabets in original order, followed by all numbers in reverse order.";
                    transformed.addAll(seqLetters);
                    List<String> revNums = new ArrayList<>(seqNumbers);
                    Collections.reverse(revNums);
                    transformed.addAll(revNums);
                    break;
                case 3:
                    ruleId = "NUMBERS_LETTERS_REVERSE";
                    ruleText = "Enter all numbers in original order, followed by all alphabets in reverse order.";
                    transformed.addAll(seqNumbers);
                    List<String> revLets = new ArrayList<>(seqLetters);
                    Collections.reverse(revLets);
                    transformed.addAll(revLets);
                    break;
                default:
                    ruleId = "REVERSE_COMPLETE";
                    ruleText = "Enter the complete sequence in reverse order.";
                    transformed.addAll(sequence);
                    Collections.reverse(transformed);
                    break;
            }

            String json = buildJson(1, sequence, ruleId, ruleText, transformed, 15);
            session.setAttribute("level14p1", json);
            out.print(json);

        } else if (part == 2) {
            // Part 2: 8 elements containing Letters, Numbers, and Symbols
            int total = 8;
            List<String> lList = pickRandom(LETTERS, 3);
            List<String> nList = pickRandom(NUMBERS, 3);
            List<String> sList = pickRandom(SYMBOLS, 2);

            List<String> sequence = new ArrayList<>();
            sequence.addAll(lList);
            sequence.addAll(nList);
            sequence.addAll(sList);
            Collections.shuffle(sequence, RAND);

            int ruleType = RAND.nextInt(5);
            String ruleId;
            String ruleText;
            List<String> transformed = new ArrayList<>();

            List<String> seqL = new ArrayList<>();
            List<String> seqN = new ArrayList<>();
            List<String> seqS = new ArrayList<>();
            for (String item : sequence) {
                if (isSymbol(item)) seqS.add(item);
                else if (isLetter(item)) seqL.add(item);
                else seqN.add(item);
            }

            switch (ruleType) {
                case 0:
                    ruleId = "EVEN_THEN_ODD";
                    ruleText = "Enter every second item (positions 2, 4, 6, 8) first, followed by the remaining items (positions 1, 3, 5, 7).";
                    // 1-indexed even positions (index 1, 3, 5, 7)
                    for (int i = 1; i < sequence.size(); i += 2) transformed.add(sequence.get(i));
                    // 1-indexed odd positions (index 0, 2, 4, 6)
                    for (int i = 0; i < sequence.size(); i += 2) transformed.add(sequence.get(i));
                    break;
                case 1:
                    ruleId = "SYMBOLS_LETTERS_NUMBERS";
                    ruleText = "Enter all Symbols first, then Alphabets, then Numbers (maintaining original order within each group).";
                    transformed.addAll(seqS);
                    transformed.addAll(seqL);
                    transformed.addAll(seqN);
                    break;
                case 2:
                    ruleId = "NUMBERS_SYMBOLS_LETTERS";
                    ruleText = "Enter all Numbers first, then Symbols, then Alphabets (maintaining original order within each group).";
                    transformed.addAll(seqN);
                    transformed.addAll(seqS);
                    transformed.addAll(seqL);
                    break;
                case 3:
                    ruleId = "LETTERS_NUMBERS_SYMBOLS";
                    ruleText = "Enter all Alphabets first, then Numbers, then Symbols (maintaining original order within each group).";
                    transformed.addAll(seqL);
                    transformed.addAll(seqN);
                    transformed.addAll(seqS);
                    break;
                default:
                    ruleId = "REVERSE_COMPLETE";
                    ruleText = "Enter the entire sequence in reverse order.";
                    transformed.addAll(sequence);
                    Collections.reverse(transformed);
                    break;
            }

            String json = buildJson(2, sequence, ruleId, ruleText, transformed, 15);
            session.setAttribute("level14p2", json);
            out.print(json);

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

    private boolean isLetter(String s) {
        return s.length() == 1 && Character.isLetter(s.charAt(0));
    }

    private boolean isSymbol(String s) {
        for (String sym : SYMBOLS) {
            if (sym.equals(s)) return true;
        }
        return false;
    }

    private List<String> pickRandom(String[] pool, int count) {
        List<String> list = new ArrayList<>();
        for (String s : pool) list.add(s);
        Collections.shuffle(list, RAND);
        return new ArrayList<>(list.subList(0, Math.min(count, list.size())));
    }

    private String buildJson(int part, List<String> sequence, String ruleId, String ruleText, List<String> transformed, int memorizeTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"part\":").append(part)
          .append(",\"memorizeTime\":").append(memorizeTime)
          .append(",\"ruleId\":\"").append(escapeJson(ruleId)).append("\"")
          .append(",\"ruleText\":\"").append(escapeJson(ruleText)).append("\"")
          .append(",\"sequence\":[");
        for (int i = 0; i < sequence.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(sequence.get(i))).append("\"");
        }
        sb.append("],\"transformed\":[");
        for (int i = 0; i < transformed.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(transformed.get(i))).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
