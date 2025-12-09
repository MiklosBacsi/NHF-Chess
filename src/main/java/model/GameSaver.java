package model;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles IO operations to save and read in chess match in JSON format.
 * @author Miklós Bácsi
 */
public class GameSaver {

    private static final String SAVE_DIR = "saved_games";

    /**
     * Static function to save a game with given properties.
     * @param variant name of the variant
     * @param result outcome of the match (as text)
     * @param moves list of moves made
     */
    public static void saveGame(String variant, String result, List<Move> moves) {
        try {
            Files.createDirectories(Paths.get(SAVE_DIR));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = SAVE_DIR + "/game_" + timestamp + ".json";

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"variant\": \"").append(variant).append("\",\n");
            json.append("  \"date\": \"").append(LocalDateTime.now().toString()).append("\",\n");
            json.append("  \"result\": \"").append(result).append("\",\n");
            json.append("  \"moves\": [\n");

            // Write Moves as JSON Objects
            for (int i = 0; i < moves.size(); i++) {
                Move m = moves.get(i);
                json.append("    {");
                json.append("\"sr\":").append(m.startRow()).append(", ");
                json.append("\"sc\":").append(m.startCol()).append(", ");
                json.append("\"er\":").append(m.endRow()).append(", ");
                json.append("\"ec\":").append(m.endCol()).append(", ");
                json.append("\"t\":\"").append(m.type()).append("\"");

                if (m.promotionType() != null) {
                    json.append(", \"p\":\"").append(m.promotionType()).append("\"");
                }

                if (m.type() == MoveType.DROP) {
                    json.append(", \"d\":\"").append(m.piece().getType()).append("\"");
                }

                json.append("}");
                if (i < moves.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n");
            json.append("}");

            Files.writeString(Paths.get(filename), json.toString());
            System.out.println("Game saved: " + filename); // Log

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Static function to load all the saved games.
     * @return list of all the saved games
     */
    public static List<GameRecord> loadAllGames() {
        List<GameRecord> records = new ArrayList<>();
        File folder = new File(SAVE_DIR);
        if (!folder.exists()) return records;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return records;

        for (File f : files) {
            try {
                records.add(parseGame(f));
            } catch (Exception e) {
                System.err.println("Failed to parse: " + f.getName());
                e.printStackTrace();
            }
        }
        records.sort((a, b) -> b.date().compareTo(a.date()));
        return records;
    }

    /**
     * Deletes given game (file).
     * @param filename name of the file (game) to be deleted
     */
    public static void deleteGame(String filename) {
        try {
            Files.deleteIfExists(Paths.get(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Robust Parser using Regex that return a saved game from a (JSON) file.
     * @param file JSON file that contains the game.
     * @return the game after reading it in
     * @throws IOException if the file is invalid
     */
    private static GameRecord parseGame(File file) throws IOException {
        String content = Files.readString(file.toPath());

        String variant = extractValue(content, "variant");
        String dateStr = extractValue(content, "date");
        String result = extractValue(content, "result");
        LocalDateTime date = LocalDateTime.parse(dateStr);

        List<SavedMove> moves = new ArrayList<>();

        // Extract the array content
        int moveStart = content.indexOf("\"moves\": [");
        if (moveStart != -1) {
            String arrayContent = content.substring(moveStart);

            // Regex to parse individual move objects: {"sr":6, "sc":4, ...}
            Pattern p = Pattern.compile("\\{.*?\\}");
            Matcher m = p.matcher(arrayContent);

            while (m.find()) {
                String obj = m.group();
                try {
                    int sr = extractInt(obj, "sr");
                    int sc = extractInt(obj, "sc");
                    int er = extractInt(obj, "er");
                    int ec = extractInt(obj, "ec");

                    String type = extractValue(obj, "t");

                    String promo = extractValue(obj, "p"); // Returns "Unknown" if missing
                    if (promo.equals("Unknown")) promo = null;

                    // Extract Drop Type
                    String drop = extractValue(obj, "d");
                    if (drop.equals("Unknown")) drop = null;

                    moves.add(new SavedMove(sr, sc, er, ec, type, promo, drop));
                } catch (Exception e) {
                    System.err.println("Skipping malformed move: " + obj);
                    e.printStackTrace();
                }
            }
        }

        return new GameRecord(file.getPath(), variant, date, result, moves);
    }

    /**
     * Helper for strings to extract value.
     * @param source we want to extract the information out of this
     * @param key we want to extract the value of this key
     * @return value of the key
     */
    private static String extractValue(String source, String key) {
        // Looks for "key": "value"
        String keyPattern = "\"" + key + "\"";
        int keyStart = source.indexOf(keyPattern);
        if (keyStart == -1) return "Unknown";

        // Find Colon after key
        int colon = source.indexOf(":", keyStart + keyPattern.length());
        if (colon == -1) return "Unknown";

        // Find Value Start (First quote after colon)
        int valStart = source.indexOf("\"", colon);
        if (valStart == -1) return "Unknown";

        // Find Value End
        int valEnd = source.indexOf("\"", valStart + 1);
        if (valEnd == -1) return "Unknown";

        return source.substring(valStart + 1, valEnd);
    }

    /**
     * Helper to extract integer value.
     * @param source we want to extract information from this
     * @param key we want to extract its (integer) value
     * @return integer value of the key
     */
    private static int extractInt(String source, String key) {
        // Looks for "key": 123
        String search = "\"" + key + "\":";
        int start = source.indexOf(search);
        if (start == -1) return 0;

        start += search.length();
        // Skip whitespace
        while (start < source.length() && Character.isWhitespace(source.charAt(start))) {
            start++;
        }

        // Find end of number (comma or closing brace)
        int end = start;

        // Check for negative sign first
        if (end < source.length() && source.charAt(end) == '-') {
            end++;
        }

        while (end < source.length() && Character.isDigit(source.charAt(end))) {
            end++;
        }

        String numStr = source.substring(start, end);

        // Safety check if empty (e.g. malformed JSON)
        if (numStr.isEmpty() || numStr.equals("-")) return 0;

        return Integer.parseInt(numStr);
    }
}