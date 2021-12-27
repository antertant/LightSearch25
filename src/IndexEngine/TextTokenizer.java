package IndexEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextTokenizer {
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
}
