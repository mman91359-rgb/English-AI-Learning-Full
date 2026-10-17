package com.english.ai.exercise.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class GrammarRuleChecker {

    private GrammarRuleChecker() {
    }

    private static final Pattern ARTICLE_A = Pattern.compile("\\b(a\\s+[aeiouAEIOU])|\\b(an\\s+[^aeiouAEIOU])");
    private static final Pattern PLURAL_NOUN = Pattern.compile("\\b(\\w+)s\\b");
    private static final Pattern THIRD_PERSON_VERB = Pattern.compile("\\b(he|she|it)\\s+\\w+(?<!s)\\b");
    private static final Pattern SUBJECT_VERB_AGREEMENT = Pattern.compile("\\b(they|we|you|I)\\s+\\w+s\\b");
    private static final Pattern PAST_TENSE_REGEX = Pattern.compile("\\b\\w+ed\\b");
    private static final Pattern INFINITIVE_TO_VERB = Pattern.compile("\\bto\\s+\\w+ing\\b");
    private static final Pattern PREPOSITION_ERROR = Pattern.compile("\\b(on\\s+school|in\\s+the\\s+bus|at\\s+home\\b)");

    private static final Map<String, String> PREPOSITION_RULES = new HashMap<>();
    private static final Map<String, String> ARTICLE_RULES = new HashMap<>();
    private static final Map<String, String> TENSE_RULES = new HashMap<>();

    static {
        PREPOSITION_RULES.put("on school", "通常说 \"at school\" 而不是 \"on school\"");
        PREPOSITION_RULES.put("in the bus", "通常说 \"on the bus\" 而不是 \"in the bus\"");
        PREPOSITION_RULES.put("at home", "正确的表达是 \"at home\"");
        
        ARTICLE_RULES.put("a ", "以元音音素开头的单词前应该用 \"an\"");
        ARTICLE_RULES.put("an ", "以辅音音素开头的单词前应该用 \"a\"");
        
        TENSE_RULES.put("ed", "注意动词过去式的拼写");
        TENSE_RULES.put("ing", "注意动词-ing形式的用法");
    }

    public static String checkGrammarRules(String userAnswer, String context) {
        if (userAnswer == null || userAnswer.isEmpty()) {
            return null;
        }
        
        userAnswer = userAnswer.toLowerCase().trim();
        
        if (ARTICLE_A.matcher(userAnswer).find()) {
            return "冠词使用错误：检查 \"a\" 和 \"an\" 的用法，元音音素前用 \"an\"";
        }
        
        if (THIRD_PERSON_VERB.matcher(userAnswer).find()) {
            return "主谓一致：主语是第三人称单数（he/she/it）时，动词需要加 \"s\"";
        }
        
        if (SUBJECT_VERB_AGREEMENT.matcher(userAnswer).find()) {
            return "主谓一致：主语是复数（they/we/you）或第一人称（I）时，动词不需要加 \"s\"";
        }
        
        if (INFINITIVE_TO_VERB.matcher(userAnswer).find()) {
            return "不定式用法：\"to\" 后面应该接动词原形，而不是动词-ing形式";
        }
        
        return null;
    }

    public static String checkPreposition(String userAnswer) {
        if (userAnswer == null || userAnswer.isEmpty()) {
            return null;
        }
        
        userAnswer = userAnswer.toLowerCase().trim();
        
        for (Map.Entry<String, String> rule : PREPOSITION_RULES.entrySet()) {
            if (userAnswer.contains(rule.getKey())) {
                return rule.getValue();
            }
        }
        
        return null;
    }

    public static String checkArticle(String userAnswer, String noun) {
        if (userAnswer == null || userAnswer.isEmpty() || noun == null || noun.isEmpty()) {
            return null;
        }
        
        String lowerNoun = noun.toLowerCase();
        boolean startsWithVowel = lowerNoun.matches("^[aeiou].*");
        
        if (userAnswer.contains("a " + noun) && startsWithVowel) {
            return "注意：\"" + noun + "\" 以元音音素开头，应该用 \"an\" 而不是 \"a\"";
        }
        
        if (userAnswer.contains("an " + noun) && !startsWithVowel) {
            return "注意：\"" + noun + "\" 以辅音音素开头，应该用 \"a\" 而不是 \"an\"";
        }
        
        return null;
    }

    public static String checkPlural(String userAnswer, String noun) {
        if (userAnswer == null || userAnswer.isEmpty() || noun == null || noun.isEmpty()) {
            return null;
        }
        
        String lowerAnswer = userAnswer.toLowerCase();
        String lowerNoun = noun.toLowerCase();
        
        if (lowerAnswer.contains(lowerNoun + "s") && isUncountableNoun(noun)) {
            return "\"" + noun + "\" 是不可数名词，通常不用复数形式";
        }
        
        return null;
    }

    private static boolean isUncountableNoun(String noun) {
        String[] uncountable = {"advice", "bread", "equipment", "furniture", "information", 
                               "knowledge", "money", "news", "progress", "research", 
                               "rice", "water", "milk", "sugar", "traffic"};
        for (String word : uncountable) {
            if (word.equalsIgnoreCase(noun)) {
                return true;
            }
        }
        return false;
    }

    public static String checkTense(String userAnswer, String context) {
        if (userAnswer == null || userAnswer.isEmpty()) {
            return null;
        }
        
        if (context != null && context.contains("yesterday") && !userAnswer.contains("ed")) {
            return "时间状语是 \"yesterday\"，句子应该使用过去时态";
        }
        
        if (context != null && context.contains("now") && !userAnswer.contains("ing")) {
            return "时间状语是 \"now\"，句子应该使用现在进行时态";
        }
        
        if (context != null && context.contains("since") && !userAnswer.contains("has") && !userAnswer.contains("have")) {
            return "时间状语是 \"since\"，句子应该使用现在完成时态";
        }
        
        return null;
    }
}