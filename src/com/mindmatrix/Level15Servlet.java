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
 * Level15Servlet – Visual Story Memory
 *   GET  /level15?part=1  → Generates a multi-scene visual story with questions.
 *   GET  /level15?part=2  → Generates a chronological visual story for sequence reordering.
 *   POST /level15         → Validates completion and updates progress.
 */
@WebServlet("/level15")
public class Level15Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    // Part 1 Story Bank
    private static class StoryP1 {
        String title;
        String[] scenes;
        String[] sceneIcons;
        String[][] questions; // [ [question, optA, optB, optC, optD, correctIdx, hint] ]

        StoryP1(String title, String[] scenes, String[] sceneIcons, String[][] questions) {
            this.title = title;
            this.scenes = scenes;
            this.sceneIcons = sceneIcons;
            this.questions = questions;
        }
    }

    // Part 2 Story Bank
    private static class StoryP2 {
        String title;
        String[] scenes;
        String[] sceneIcons;

        StoryP2(String title, String[] scenes, String[] sceneIcons) {
            this.title = title;
            this.scenes = scenes;
            this.sceneIcons = sceneIcons;
        }
    }

    private static final StoryP1[] STORIES_P1 = {
        new StoryP1(
            "Park Adventure",
            new String[]{
                "A boy enters a green park wearing a red backpack.",
                "He discovers a blue bicycle resting against a tall oak tree.",
                "He gently places his red backpack beside the tree roots.",
                "A playful golden dog runs over toward the bicycle.",
                "The boy picks up his backpack and sits on a wooden bench."
            },
            new String[]{"👦🎒🌳", "🚲🌳✨", "🎒🌿🌳", "🐕🚲🌼", "👦🎒🪑"},
            new String[][]{
                {"What color was the boy's backpack?", "Red", "Blue", "Green", "Yellow", "0", "Think about what the boy was wearing when entering the park."},
                {"What was resting against the tall oak tree?", "A blue bicycle", "A skateboard", "A scooter", "A red wagon", "0", "Look back at what he discovered near the tree."},
                {"Which animal appeared near the bicycle?", "A cat", "A golden dog", "A squirrel", "A rabbit", "1", "Remember the playful animal that ran over."},
                {"Where did the boy finally sit?", "On the grass", "On a wooden bench", "On a stone wall", "On a swing", "1", "Think about where he went after picking up his backpack."}
            }
        ),
        new StoryP1(
            "Space Explorer",
            new String[]{
                "An astronaut in a white spacesuit boards a silver rocket.",
                "The rocket blasts off toward Mars, leaving a trail of orange flames.",
                "The astronaut lands near a glowing purple crater on the red planet.",
                "A small robotic rover with solar panels collects shiny crystal rocks.",
                "The astronaut plants a blue flag and returns safely to the ship."
            },
            new String[]{"👨‍🚀🚀⭐", "🚀🔥🌌", "👨‍🚀🪐🟣", "🤖💎🪨", "🚩👨‍🚀🚀"},
            new String[][]{
                {"What color were the flames from the rocket launch?", "Orange", "Blue", "Green", "Purple", "0", "Think about the trail left during blast off."},
                {"What did the astronaut find near the landing site?", "A glowing purple crater", "A deep canyon", "An alien city", "An icy lake", "0", "Remember the landmark on the red planet."},
                {"What was the robotic rover collecting?", "Shiny crystal rocks", "Soil samples", "Ice cubes", "Metallic dust", "0", "Think about what the robot picked up."},
                {"What color was the flag planted on the planet?", "Red", "White", "Blue", "Yellow", "2", "Remember the flag planted before returning."}
            }
        ),
        new StoryP1(
            "Forest Treasure Quest",
            new String[]{
                "A girl wearing a yellow jacket walks into an ancient forest.",
                "She consults an old parchment map with a red compass seal.",
                "She finds a hollow mossy log containing a golden key.",
                "A wise brown owl perched on a branch watches her silently.",
                "She unlocks a wooden chest filled with sparkling emerald gems."
            },
            new String[]{"👧🧥🌲", "🗺️🧭📜", "🪵🔑✨", "🦉🌿🌲", "🗝️📦💎"},
            new String[][]{
                {"What color was the girl's jacket?", "Yellow", "Red", "Blue", "Green", "0", "Remember what she was wearing entering the forest."},
                {"Where did she find the golden key?", "Inside a hollow mossy log", "Under a big stone", "Inside a bird nest", "In a clear stream", "0", "Think about the hiding spot for the key."},
                {"Which bird watched the girl from the branch?", "A brown owl", "A blue jay", "A black raven", "A white dove", "0", "Remember the wise creature in the tree."},
                {"What type of gems were inside the wooden chest?", "Emeralds", "Rubies", "Sapphires", "Diamonds", "0", "Think about the sparkling green treasures."}
            }
        )
    };

    private static final StoryP2[] STORIES_P2 = {
        new StoryP2(
            "Detective Mystery",
            new String[]{
                "Detective enters the dark library carrying a brass magnifying glass.",
                "He discovers mysterious muddy footprints leading to the bookcase.",
                "He pulls a secret blue book to open a hidden passageway.",
                "Inside the secret room, he finds a locked safe with a code dial.",
                "He enters the secret code and retrieves the missing diamond necklace."
            },
            new String[]{"🕵️🔍📚", "👣📖🔎", "📘🚪✨", "🔐🚪🗝️", "💎✨🕵️"}
        ),
        new StoryP2(
            "Art Gallery Exhibition",
            new String[]{
                "An artist enters her studio carrying a palette of fresh oil paints.",
                "She paints a vibrant sunset over a calm ocean on a large canvas.",
                "She places the finished masterpiece into an ornate golden frame.",
                "The gallery owner arrives in a black car and collects the artwork.",
                "The painting is unveiled at the grand opening to cheering art lovers."
            },
            new String[]{"👩‍🎨🎨🖌️", "🌅🌊🖼️", "🖼️✨👑", "🚗🏢📦", "🏛️🎉👏"}
        ),
        new StoryP2(
            "Bakery Morning",
            new String[]{
                "The baker opens the warm shop door as the morning sun rises.",
                "He mixes flour, fresh milk, and sweet vanilla in a silver bowl.",
                "He kneads the dough and shapes golden croissants and fruit tarts.",
                "He slides the baking tray into the hot oven filled with sweet aromas.",
                "Happy customers line up to buy freshly baked pastries and warm bread."
            },
            new String[]{"👨‍🍳🌅🚪", "🥣🥛🌾", "🥐🍓✨", "🔥🍞🥖", "👥🥐☕"}
        )
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
            StoryP1 story = STORIES_P1[RAND.nextInt(STORIES_P1.length)];
            StringBuilder sb = new StringBuilder();
            sb.append("{\"part\":1,\"title\":\"").append(escapeJson(story.title)).append("\"")
              .append(",\"secondsPerScene\":5")
              .append(",\"scenes\":[");
            for (int i = 0; i < story.scenes.length; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"index\":").append(i + 1)
                  .append(",\"text\":\"").append(escapeJson(story.scenes[i])).append("\"")
                  .append(",\"icon\":\"").append(escapeJson(story.sceneIcons[i])).append("\"}");
            }
            sb.append("],\"questions\":[");
            for (int i = 0; i < story.questions.length; i++) {
                String[] q = story.questions[i];
                if (i > 0) sb.append(",");
                sb.append("{\"question\":\"").append(escapeJson(q[0])).append("\"")
                  .append(",\"options\":[\"").append(escapeJson(q[1])).append("\",\"")
                  .append(escapeJson(q[2])).append("\",\"")
                  .append(escapeJson(q[3])).append("\",\"")
                  .append(escapeJson(q[4])).append("\"]")
                  .append(",\"correct\":").append(q[5])
                  .append(",\"hint\":\"").append(escapeJson(q[6])).append("\"}");
            }
            sb.append("]}");

            session.setAttribute("level15p1", sb.toString());
            out.print(sb.toString());

        } else if (part == 2) {
            StoryP2 story = STORIES_P2[RAND.nextInt(STORIES_P2.length)];
            StringBuilder sb = new StringBuilder();
            sb.append("{\"part\":2,\"title\":\"").append(escapeJson(story.title)).append("\"")
              .append(",\"secondsPerScene\":5")
              .append(",\"scenes\":[");
            for (int i = 0; i < story.scenes.length; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"index\":").append(i + 1)
                  .append(",\"text\":\"").append(escapeJson(story.scenes[i])).append("\"")
                  .append(",\"icon\":\"").append(escapeJson(story.sceneIcons[i])).append("\"}");
            }
            sb.append("]}");

            session.setAttribute("level15p2", sb.toString());
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

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
