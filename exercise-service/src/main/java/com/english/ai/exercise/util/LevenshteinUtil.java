package com.english.ai.exercise.util;

public class LevenshteinUtil {

    private LevenshteinUtil() {
    }

    public static int calculateDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }
        s1 = s1.toLowerCase().trim();
        s2 = s2.toLowerCase().trim();
        
        if (s1.isEmpty()) return s2.length();
        if (s2.isEmpty()) return s1.length();
        
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        s1 = s1.toLowerCase().trim();
        s2 = s2.toLowerCase().trim();
        
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        
        int distance = calculateDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        return 1.0 - (double) distance / maxLength;
    }

    public static boolean isSpellingError(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }
        userAnswer = userAnswer.toLowerCase().trim();
        correctAnswer = correctAnswer.toLowerCase().trim();
        
        if (userAnswer.isEmpty() || correctAnswer.isEmpty()) {
            return false;
        }
        
        double similarity = calculateSimilarity(userAnswer, correctAnswer);
        return similarity >= 0.8 && similarity < 1.0;
    }

    public static String generateSpellingHint(String userAnswer, String correctAnswer) {
        if (!isSpellingError(userAnswer, correctAnswer)) {
            return null;
        }
        return "你输入的单词 '" + userAnswer + "' 与正确答案 '" + correctAnswer + "' 非常相似，检查一下拼写是否正确？";
    }
}