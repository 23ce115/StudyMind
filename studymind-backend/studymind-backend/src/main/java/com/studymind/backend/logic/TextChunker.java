package com.studymind.backend.logic;

import java.util.ArrayList;
import java.util.List;

/** Splits extracted PDF page text into overlapping, sentence-aware chunks that remember their page. */
public final class TextChunker {
    private TextChunker() {}

    public record Chunk(int page, int index, String text) {}

    public static String normalize(String s) {
        if (s == null) return "";
        return s.replace("\u0000", "").replace("\r", "")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll(" ?\n ?", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    /** @param pages text of each page, index 0 = page 1 */
    public static List<Chunk> chunk(List<String> pages, int target, int overlap) {
        List<Chunk> out = new ArrayList<>();
        int idx = 0;
        for (int p = 0; p < pages.size(); p++) {
            String t = normalize(pages.get(p));
            if (t.length() < 30) continue;
            int start = 0;
            while (start < t.length()) {
                int end = Math.min(t.length(), start + target);
                if (end < t.length()) {
                    int cut = boundary(t, start + target / 2, end);
                    if (cut > start) end = cut;
                }
                String piece = t.substring(start, end).trim();
                if (piece.length() >= 30) out.add(new Chunk(p + 1, idx++, piece));
                if (end >= t.length()) break;
                start = Math.max(end - overlap, start + 1);
            }
        }
        return out;
    }

    /** Last sentence end (or, failing that, whitespace) in [from, end]. */
    private static int boundary(String t, int from, int end) {
        for (int i = end - 1; i >= from; i--) {
            char c = t.charAt(i);
            if ((c == '.' || c == '?' || c == '!' || c == '\n') && i + 1 < t.length() && Character.isWhitespace(t.charAt(i + 1))) return i + 1;
        }
        for (int i = end - 1; i >= from; i--) if (Character.isWhitespace(t.charAt(i))) return i;
        return -1;
    }
}
