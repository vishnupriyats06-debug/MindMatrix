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
 * Level17Servlet – Advanced Memory Challenge
 * Part 1: Relationship Reconstruction (Person -> Object -> Location)
 * Part 2: Memory Calculation Challenge (Object -> Number, followed by Addition, Subtraction, Multiplication)
 */
@WebServlet("/level17")
public class Level17Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Person definitions
    private static final String[][] PEOPLE_POOL = {
        {"👩", "Anu"},
        {"👨", "Ravi"},
        {"👧", "Maya"},
        {"👦", "Arun"},
        {"👩‍🦰", "Priya"},
        {"👨‍🦱", "Kiran"},
        {"👵", "Leela"},
        {"👴", "Suresh"}
    };

    // Object definitions
    private static final String[][] OBJECTS_POOL = {
        {"🎒", "Bag"},
        {"📱", "Phone"},
        {"📕", "Book"},
        {"🎧", "Headphones"},
        {"⌚", "Watch"},
        {"📷", "Camera"},
        {"💻", "Laptop"},
        {"🔑", "Key"}
    };

    // Location definitions
    private static final String[][] LOCATIONS_POOL = {
        {"🪑", "Chair"},
        {"🛋️", "Sofa"},
        {"🪟", "Window"},
        {"🚪", "Door"},
        {"🪵", "Table"},
        {"🪴", "Balcony"},
        {"🛏️", "Bed"},
        {"📚", "Shelf"}
    };

    // Symbol pool for Part 2
    private static final String[] SYMBOLS_POOL = {"🍎", "⭐", "🔵", "🔺", "🟩", "💎", "🌙", "⚡", "🍀", "🍇"};

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
        int count = 4 + rand.nextInt(2); // 4 or 5 relationships

        List<String[]> people = new ArrayList<>(Arrays.asList(PEOPLE_POOL));
        List<String[]> objects = new ArrayList<>(Arrays.asList(OBJECTS_POOL));
        List<String[]> locations = new ArrayList<>(Arrays.asList(LOCATIONS_POOL));

        Collections.shuffle(people, rand);
        Collections.shuffle(objects, rand);
        Collections.shuffle(locations, rand);

        StringBuilder relsJson = new StringBuilder("[");
        StringBuilder peopleJson = new StringBuilder("[");
        StringBuilder objectsJson = new StringBuilder("[");
        StringBuilder locationsJson = new StringBuilder("[");

        List<String> chosenPeople = new ArrayList<>();
        List<String> chosenObjects = new ArrayList<>();
        List<String> chosenLocations = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String pStr = people.get(i)[0] + " " + people.get(i)[1];
            String oStr = objects.get(i)[0] + " " + objects.get(i)[1];
            String lStr = locations.get(i)[0] + " " + locations.get(i)[1];

            chosenPeople.add(pStr);
            chosenObjects.add(oStr);
            chosenLocations.add(lStr);

            if (i > 0) relsJson.append(",");
            relsJson.append("{")
                    .append("\"person\":\"").append(escapeJson(pStr)).append("\",")
                    .append("\"object\":\"").append(escapeJson(oStr)).append("\",")
                    .append("\"location\":\"").append(escapeJson(lStr)).append("\"")
                    .append("}");
        }
        relsJson.append("]");

        // Shuffle options for reconstruction stage
        List<String> shuffledPeople = new ArrayList<>(chosenPeople);
        List<String> shuffledObjects = new ArrayList<>(chosenObjects);
        List<String> shuffledLocations = new ArrayList<>(chosenLocations);
        Collections.shuffle(shuffledPeople, rand);
        Collections.shuffle(shuffledObjects, rand);
        Collections.shuffle(shuffledLocations, rand);

        for (int i = 0; i < shuffledPeople.size(); i++) {
            if (i > 0) peopleJson.append(",");
            peopleJson.append("\"").append(escapeJson(shuffledPeople.get(i))).append("\"");
        }
        peopleJson.append("]");

        for (int i = 0; i < shuffledObjects.size(); i++) {
            if (i > 0) objectsJson.append(",");
            objectsJson.append("\"").append(escapeJson(shuffledObjects.get(i))).append("\"");
        }
        objectsJson.append("]");

        for (int i = 0; i < shuffledLocations.size(); i++) {
            if (i > 0) locationsJson.append(",");
            locationsJson.append("\"").append(escapeJson(shuffledLocations.get(i))).append("\"");
        }
        locationsJson.append("]");

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"part\":1,");
        sb.append("\"memorizeTime\":15,");
        sb.append("\"relationships\":").append(relsJson).append(",");
        sb.append("\"shuffledPeople\":").append(peopleJson).append(",");
        sb.append("\"shuffledObjects\":").append(objectsJson).append(",");
        sb.append("\"shuffledLocations\":").append(locationsJson);
        sb.append("}");

        return sb.toString();
    }

    private String generatePart2Data() {
        Random rand = new Random();
        List<String> symbols = new ArrayList<>(Arrays.asList(SYMBOLS_POOL));
        Collections.shuffle(symbols, rand);

        int symbolCount = 5;
        // Assign distinct positive integers 2 to 9 to ensure clean calculations
        List<Integer> possibleVals = new ArrayList<>(Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(possibleVals, rand);

        Map<String, Integer> symMap = new LinkedHashMap<>();
        StringBuilder symJson = new StringBuilder("[");

        for (int i = 0; i < symbolCount; i++) {
            String sym = symbols.get(i);
            int val = possibleVals.get(i);
            symMap.put(sym, val);

            if (i > 0) symJson.append(",");
            symJson.append("{")
                  .append("\"symbol\":\"").append(escapeJson(sym)).append("\",")
                  .append("\"value\":").append(val)
                  .append("}");
        }
        symJson.append("]");

        List<String> keyList = new ArrayList<>(symMap.keySet());

        // 1. Addition Questions (2 to 3 items)
        StringBuilder addJson = new StringBuilder("[");
        for (int q = 0; q < 3; q++) {
            int termsCount = (q == 0) ? 2 : (q == 1 ? 3 : 3);
            Collections.shuffle(keyList, rand);
            List<String> qSyms = new ArrayList<>();
            int ans = 0;
            for (int t = 0; t < termsCount; t++) {
                String s = keyList.get(t);
                qSyms.add(s);
                ans += symMap.get(s);
            }
            if (q > 0) addJson.append(",");
            addJson.append("{")
                   .append("\"expr\":\"").append(String.join(" + ", qSyms)).append("\",")
                   .append("\"symbols\":[").append(toJsonArray(qSyms)).append("],")
                   .append("\"answer\":").append(ans)
                   .append("}");
        }
        addJson.append("]");

        // 2. Subtraction Questions (Ensure answer >= 0)
        StringBuilder subJson = new StringBuilder("[");
        for (int q = 0; q < 3; q++) {
            // Sort chosen items descending by value so answer is non-negative
            Collections.shuffle(keyList, rand);
            String firstSym = keyList.get(0);
            int firstVal = symMap.get(firstSym);

            // Pick 1 or 2 smaller elements
            List<String> smaller = new ArrayList<>();
            for (String s : keyList) {
                if (!s.equals(firstSym) && symMap.get(s) <= firstVal) {
                    smaller.add(s);
                }
            }

            if (smaller.isEmpty()) {
                // If firstSym was smallest, swap with largest
                String maxSym = keyList.get(0);
                for (String s : keyList) {
                    if (symMap.get(s) > symMap.get(maxSym)) maxSym = s;
                }
                firstSym = maxSym;
                firstVal = symMap.get(firstSym);
                for (String s : keyList) {
                    if (!s.equals(firstSym)) smaller.add(s);
                }
            }

            Collections.shuffle(smaller, rand);
            List<String> subTerms = new ArrayList<>();
            subTerms.add(firstSym);
            int subAns = firstVal;

            int count = (q == 0 || smaller.size() < 2) ? 1 : 2;
            for (int t = 0; t < count && t < smaller.size(); t++) {
                int sv = symMap.get(smaller.get(t));
                if (subAns - sv >= 0) {
                    subTerms.add(smaller.get(t));
                    subAns -= sv;
                }
            }

            if (subTerms.size() == 1 && !smaller.isEmpty()) {
                subTerms.add(smaller.get(0));
                subAns = Math.max(0, firstVal - symMap.get(smaller.get(0)));
            }

            if (q > 0) subJson.append(",");
            subJson.append("{")
                   .append("\"expr\":\"").append(String.join(" - ", subTerms)).append("\",")
                   .append("\"symbols\":[").append(toJsonArray(subTerms)).append("],")
                   .append("\"answer\":").append(subAns)
                   .append("}");
        }
        subJson.append("]");

        // 3. Multiplication Questions (2 items, answer = val1 * val2)
        StringBuilder mulJson = new StringBuilder("[");
        for (int q = 0; q < 3; q++) {
            Collections.shuffle(keyList, rand);
            String s1 = keyList.get(0);
            String s2 = keyList.get(1);
            int ans = symMap.get(s1) * symMap.get(s2);

            if (q > 0) mulJson.append(",");
            mulJson.append("{")
                   .append("\"expr\":\"").append(s1).append(" × ").append(s2).append("\",")
                   .append("\"symbols\":[").append(toJsonArray(Arrays.asList(s1, s2))).append("],")
                   .append("\"answer\":").append(ans)
                   .append("}");
        }
        mulJson.append("]");

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"part\":2,");
        sb.append("\"memorizeTime\":15,");
        sb.append("\"associations\":").append(symJson).append(",");
        sb.append("\"addition\":").append(addJson).append(",");
        sb.append("\"subtraction\":").append(subJson).append(",");
        sb.append("\"multiplication\":").append(mulJson);
        sb.append("}");

        return sb.toString();
    }

    private String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"ok\"}");
    }
}
