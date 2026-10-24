package com.fixlink.chatbot.adapter.out.rag;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Bm25Scorer {

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private static final Set<String> STOPWORDS = Set.of(
            "và", "là", "của", "cho", "với", "trong", "có", "được", "các", "những",
            "ở", "tại", "theo", "đến", "khi", "như", "đã", "sẽ", "đang", "thì",
            "mà", "bị", "bởi", "do", "về", "lại", "ra", "vào", "này", "đó",
            "the", "is", "at", "which", "on", "in", "and", "a", "an", "to"
    );

    public static Set<String> tokenize(String text) {
        return new HashSet<>(tokenizeList(text));
    }

    /**
     * Tách token gồm âm tiết đơn và bigram hai âm tiết liền nhau. Tiếng Việt viết tách âm tiết nên
     * từ ghép như "đặt cọc", "linh kiện", "mật khẩu" chỉ được nhận ra khi xét cặp âm tiết.
     */
    public static List<String> tokenizeList(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] syllables = Normalizer.normalize(text, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .split("[^\\p{L}\\p{N}]+");
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < syllables.length; i++) {
            String current = syllables[i];
            if (isContentWord(current)) {
                tokens.add(current);
                if (i + 1 < syllables.length && isContentWord(syllables[i + 1])) {
                    tokens.add(current + "_" + syllables[i + 1]);
                }
            }
        }
        return tokens;
    }

    private static boolean isContentWord(String term) {
        return term.length() >= 2 && !STOPWORDS.contains(term);
    }

    public static double computeScore(
            String query,
            String documentText,
            double avgDocLength,
            int totalDocs,
            Map<String, Integer> docFrequencies
    ) {
        Set<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty()) {
            return 0.0;
        }

        List<String> docTokens = tokenizeList(documentText);
        int docLength = docTokens.size();
        if (docLength == 0) {
            return 0.0;
        }

        Map<String, Integer> termFreqs = new HashMap<>();
        for (String token : docTokens) {
            termFreqs.merge(token, 1, Integer::sum);
        }

        double score = 0.0;
        for (String term : queryTerms) {
            int tf = termFreqs.getOrDefault(term, 0);
            if (tf == 0) {
                continue;
            }

            int df = docFrequencies.getOrDefault(term, 1);
            // IDF computation: ln(1 + (N - df + 0.5) / (df + 0.5))
            double idf = Math.log(1.0 + (totalDocs - df + 0.5) / (df + 0.5));
            double tfNorm = (tf * (K1 + 1.0)) / (tf + K1 * (1.0 - B + B * (docLength / Math.max(1.0, avgDocLength))));

            score += idf * tfNorm;
        }

        return Math.max(0.0, score);
    }
}
