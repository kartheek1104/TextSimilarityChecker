import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.TokenStream;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TextProcessor {

    private final Analyzer analyzer;
    private final Map<String, List<String>> synonymMap;
    private final JaroWinklerSimilarity fuzzyMatcher;
    private final LevenshteinDistance levenshtein;
    private static final double HYBRID_THRESHOLD = 0.75;

    public TextProcessor() {
        this.analyzer = new StandardAnalyzer();
        this.synonymMap = buildSynonymMapFromFile("input/synonyms.txt");
        this.fuzzyMatcher = new JaroWinklerSimilarity();
        this.levenshtein = new LevenshteinDistance();
    }

    private Map<String, List<String>> buildSynonymMapFromFile(String filepath) {
        Map<String, List<String>> map = new HashMap<>();

        try {
            List<String> lines = Files.readAllLines(Paths.get(filepath));

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] words = line.toLowerCase().split("\\s*,\\s*");

                for (String word : words) {
                    List<String> synonyms = new ArrayList<>();
                    for (String syn : words) {
                        if (!syn.equals(word)) {
                            synonyms.add(syn);
                        }
                    }
                    map.merge(word, synonyms, (oldList, newList) -> {
                        Set<String> merged = new HashSet<>(oldList);
                        merged.addAll(newList);
                        return new ArrayList<>(merged);
                    });
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading synonyms file: " + e.getMessage());
        }

        return map;
    }

    public List<String> tokenize(String text) throws IOException {
        List<String> tokens = new ArrayList<>();
        try (TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text))) {
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokens.add(attr.toString());
            }
            tokenStream.end();
        }
        return tokens;
    }

    public List<String> expandTokensWithSynonyms(List<String> tokens) {
        List<String> expanded = new ArrayList<>();
        for (String token : tokens) {
            expanded.add(token);
            List<String> syns = synonymMap.get(token.toLowerCase());
            if (syns != null) {
                expanded.addAll(syns);
            }
        }
        return expanded;
    }

    public Map<String, Integer> getTermFrequency(List<String> tokens) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String token : tokens) {
            freqMap.put(token, freqMap.getOrDefault(token, 0) + 1);
        }
        return freqMap;
    }

    public double computeCosineSimilarity(String text1, String text2) throws IOException {
        List<String> tokens1 = expandTokensWithSynonyms(tokenize(text1));
        List<String> tokens2 = expandTokensWithSynonyms(tokenize(text2));

        Map<String, Integer> tf1 = getTermFrequency(tokens1);
        Map<String, Integer> tf2 = getTermFrequency(tokens2);

        Set<String> allTokens = new HashSet<>();
        allTokens.addAll(tf1.keySet());
        allTokens.addAll(tf2.keySet());

        double dotProduct = 0.0;
        double mag1 = 0.0;
        double mag2 = 0.0;

        for (String token1 : allTokens) {
            int val1 = getMatchingTokenFrequency(token1, tf1);
            int val2 = getMatchingTokenFrequency(token1, tf2);
            dotProduct += val1 * val2;
            mag1 += val1 * val1;
            mag2 += val2 * val2;
        }

        if (mag1 == 0.0 || mag2 == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(mag1) * Math.sqrt(mag2));
    }

    private int getMatchingTokenFrequency(String target, Map<String, Integer> tfMap) {
        for (String key : tfMap.keySet()) {
            if (isSimilar(target, key)) {
                return tfMap.get(key);
            }
        }
        return 0;
    }

    private boolean isSimilar(String a, String b) {
        return computeHybridSimilarity(a, b) >= HYBRID_THRESHOLD;
    }

    public double computeHybridSimilarity(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();

        if (a.equals(b)) return 1.0;

        double jwScore = fuzzyMatcher.apply(a, b);
        int editDistance = levenshtein.apply(a, b);
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 0.0;

        double levScore = 1.0 - ((double) editDistance / maxLen);
        return 0.5 * jwScore + 0.5 * levScore;
    }

    public Set<String> getMatchingTokens(String text1, String text2) throws IOException {
        List<String> tokens1 = tokenize(text1);
        List<String> tokens2 = tokenize(text2);

        List<String> expanded2 = expandTokensWithSynonyms(tokens2);
        Set<String> matchedTokens = new HashSet<>();

        for (String token1 : tokens1) {
            for (String token2 : expanded2) {
                if (isSimilar(token1, token2)) {
                    matchedTokens.add(token1);
                    break;
                }
            }
        }

        return matchedTokens;
    }

    public Map<String, String> getTokenSimilarities(String text1, String text2) throws IOException {
        List<String> tokens1 = tokenize(text1);
        List<String> tokens2 = tokenize(text2);

        Map<String, String> result = new LinkedHashMap<>();

        for (String t1 : tokens1) {
            double maxScore = 0;
            String bestMatch = "No match";

            for (String t2 : tokens2) {
                double score = computeHybridSimilarity(t1, t2);
                if (score > maxScore) {
                    maxScore = score;
                    bestMatch = t2 + " (" + String.format("%.2f", score) + ")";
                }
            }

            result.put(t1, bestMatch);
        }

        return result;
    }

    public void close() {
        analyzer.close();
    }
}
