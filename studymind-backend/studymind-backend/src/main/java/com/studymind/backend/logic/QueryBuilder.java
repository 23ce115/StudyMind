package com.studymind.backend.logic;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns a natural-language question into a safe Postgres to_tsquery string ("a | b | c"). */
public final class QueryBuilder {
    private QueryBuilder() {}

    private static final Set<String> STOP = Set.of(
        "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "has", "have",
        "what", "when", "where", "which", "who", "whom", "why", "how", "does", "did", "this", "that", "these", "those", "with",
        "from", "into", "about", "between", "difference", "explain", "give", "tell", "me", "my", "use", "using", "example",
        "examples", "main", "any", "is", "it", "its", "in", "on", "of", "to", "a", "an", "be", "as", "at", "by", "or", "do", "if",
        "than", "then", "there", "their", "they", "them", "will", "would", "should", "could", "also", "some", "more", "most");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9]{2,}");

    public static String toTsQuery(String question) {
        if (question == null) return "";
        Set<String> terms = new LinkedHashSet<>();
        Matcher m = TOKEN.matcher(question.toLowerCase(Locale.ROOT));
        while (m.find() && terms.size() < 12) {
            String w = m.group();
            if (!STOP.contains(w)) terms.add(w);
        }
        return String.join(" | ", terms);
    }
}
