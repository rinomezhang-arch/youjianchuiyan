package com.youjian.banquet.util;

/**
 * 简单的字符串相似度打分，用来把拍照识别出来的手写品名（可能有错别字/简称/口语化写法）
 * 跟系统里 ingredient_master 的真实品名做模糊匹配。没有引入专门的NLP分词/相似度库，
 * 用编辑距离（Levenshtein）归一化成 0~1 的相似度分数，够用且没有额外依赖。
 */
public final class FuzzyMatchUtil {

    private FuzzyMatchUtil() {
    }

    /** 返回 0~1 的相似度，1 表示完全相同。 */
    public static double similarity(String a, String b) {
        if (a == null || b == null) return 0;
        a = a.trim();
        b = b.trim();
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equals(b)) return 1;
        // 一个字符串完全包含另一个（比如"土豆"包含在"土豆丝专用土豆"里），给一个较高但不满分的分数
        if (a.contains(b) || b.contains(a)) {
            int shorter = Math.min(a.length(), b.length());
            int longer = Math.max(a.length(), b.length());
            return 0.7 + 0.3 * shorter / (double) longer;
        }
        int dist = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return 1.0 - dist / (double) maxLen;
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
