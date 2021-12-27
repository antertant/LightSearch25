package Search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tokenizer {

    public static List<String> splitWords (String inputString) {
        List<String> originTokens;
        List<String> tokens = new ArrayList<>();

        originTokens = Arrays.asList(inputString.split("[\\p{Punct}\\s]+"));
        for (String token: originTokens) {
            if (!token.equals("")) {
                tokens.add(token.toLowerCase());
            }
        }

        return tokens;
    }

    public static List<List<String>> splitSentences (String inputString) {
        String[] sentenceArray = inputString.split("[.?!/<>]+");
        List<String> sentenceList = new ArrayList<>();
        List<String> headline = new ArrayList<>();

        int HEADLINE = 0;
        int TEXT = 0;
        int GRAPHIC = 0;
        for (String sentence: sentenceArray) {
            if (sentence.equals("HEADLINE")) {
                HEADLINE++;
                continue;
            }
            else if (sentence.equals("TEXT")) {
                TEXT++;
                continue;
            }
            else if (sentence.equals("GRAPHIC")) {
                GRAPHIC++;
                continue;
            }
            else if (sentence.equals("P") || sentence.equals("\n")
                    || sentence.equals(" \n") || sentence.equals("\" \n")
                    || sentence.equals(") \n") || sentence.equals("> \n")
                    || sentence.equals("] \n") || sentence.equals("} \n")) {
                continue;
            }

            if (HEADLINE == 1) {
                if (sentence.length() >= 5) {
                    headline.add(sentence);
                }
            }
            else if (TEXT == 1 || GRAPHIC == 1) {
                if (sentence.length() >= 5) {
                    sentenceList.add(sentence);
                }
            }
        }

        List<List<String>> result = new ArrayList<>();
        result.add(headline);
        result.add(sentenceList);
        return result;
    }

    public static String parseHeadline (List<List<String>> inputStringList) {
        String headlineTmp = "";
        int sizeDifference = 50, headSize;

        if (inputStringList.get(0).isEmpty()) {
            for (String line: inputStringList.get(1)) {
                for (String sentence: line.split("[\n]+")) {
                    headSize = headlineTmp.length();
                    sizeDifference = 50 - headSize;
                    if (sizeDifference == 0) { break; }
                    if (sentence.length() < sizeDifference) {
                        headlineTmp = headlineTmp.concat(sentence);
                    }
                    else {
                        headlineTmp = headlineTmp.concat(sentence.substring(0, sizeDifference));
                    }
                }
                if (sizeDifference == 0) { break; }
            }
            headlineTmp = headlineTmp.concat("... ");
        }
        else {
            for (String line : inputStringList.get(0)) {
                for (String sentence : line.split("[\n]+")) {
                    headlineTmp = headlineTmp.concat(sentence);
                }
            }
        }
        return headlineTmp;
    }

    public static String parseDate (String inputString) {
        return "(" + inputString.substring(2, 4) + "/"
                + inputString.substring(4, 6) + "/"
                + "19" + inputString.substring(6, 8) + ")";
    }
}
