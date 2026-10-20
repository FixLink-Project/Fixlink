package com.fixlink.chatbot.adapter.out.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReciprocalRankFusion {

    private static final double K = 60.0;

    /**
     * Hợp nhất danh sách xếp hạng từ Vector Search và BM25 Search bằng công thức RRF:
     * RRF_Score(item) = sum_i( 1.0 / (k + rank_i(item)) )
     */
    public static <T> Map<T, Double> fuse(List<T> ranking1, List<T> ranking2) {
        Map<T, Double> scores = new HashMap<>();

        // Ranking 1
        for (int rank = 0; rank < ranking1.size(); rank++) {
            T item = ranking1.get(rank);
            double rrf = 1.0 / (K + rank + 1);
            scores.merge(item, rrf, Double::sum);
        }

        // Ranking 2
        for (int rank = 0; rank < ranking2.size(); rank++) {
            T item = ranking2.get(rank);
            double rrf = 1.0 / (K + rank + 1);
            scores.merge(item, rrf, Double::sum);
        }

        return scores;
    }
}
