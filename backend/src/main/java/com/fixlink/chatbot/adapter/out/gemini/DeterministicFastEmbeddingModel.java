package com.fixlink.chatbot.adapter.out.gemini;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class DeterministicFastEmbeddingModel {

    public static final int DIMENSION = 256;

    /**
     * Sinh vector embedding xác định (deterministic) dựa trên n-gram và hashing MD5.
     * Tương thích hoàn toàn cho chạy test nội bộ và offline RAG mà không cần gọi mạng ngoài.
     */
    public static double[] embed(String text) {
        double[] vector = new double[DIMENSION];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String normalized = text.toLowerCase(Locale.ROOT).trim();
        String[] words = normalized.split("\\s+");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            hashIntoVector(word, vector, 1.0);

            // Bigram
            if (i + 1 < words.length) {
                hashIntoVector(word + "_" + words[i + 1], vector, 1.5);
            }
        }

        // Chuẩn hoá độ dài vector (L2 norm)
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DIMENSION; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    private static void hashIntoVector(String token, double[] vector, double weight) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < 4; i++) {
                int idx = Math.abs((hash[i * 4] << 24) | ((hash[i * 4 + 1] & 0xFF) << 16)
                        | ((hash[i * 4 + 2] & 0xFF) << 8) | (hash[i * 4 + 3] & 0xFF)) % DIMENSION;
                vector[idx] += weight;
            }
        } catch (Exception e) {
            int idx = Math.abs(token.hashCode()) % DIMENSION;
            vector[idx] += weight;
        }
    }

    public static double cosineSimilarity(double[] v1, double[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            return 0.0;
        }
        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 <= 0 || norm2 <= 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
