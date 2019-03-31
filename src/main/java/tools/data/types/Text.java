package tools.data.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Text {
    private StringBuilder value;

    public String getTextEndString() {
        return textEndString;
    }

    public void setTextEndString(String textEndString) {
        this.textEndString = textEndString;
    }

    private String textEndString = "{end_text}";

    public enum PunctuationPolicy {
        IGNORE(Pattern.compile("[\\p{Punct}\\s]+")), INCLUDE,
        EXCLUDE {
            @Override
            public String[] split(String word) {
                StringBuilder punctuation = new StringBuilder();
                int i = word.length();
                while (i > 0 && String.valueOf(word.charAt(i - 1)).matches("\\p{Punct}")) {
                    i--;
                }
                if (i == word.length() || i == 0) {
                    return new String[]{word};
                }
                return new String[]{word.substring(0, i), word.substring(i)};
            }
        };
        private Pattern delimiterPattern;

        PunctuationPolicy() {
            this(Pattern.compile("[\\s]+"));
        }

        PunctuationPolicy(Pattern regex) {
            this.delimiterPattern = regex;
        }

        public String[] split(String word) {
            return new String[]{word};
        }

        public Pattern getDelimiterPattern() {
            return delimiterPattern;
        }
    }

    public Text(String value) {
        this.value = new StringBuilder(value);
    }

    public String getString() {
        return value.toString();
    }

    public StringBuilder getValue() {
        return value;
    }

    public void setString(String value) {
        this.value = new StringBuilder(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public String[] toWords() {
        return toWords(PunctuationPolicy.IGNORE);
    }

    public String[] toWords(PunctuationPolicy punctuationPolicy) {
        return toWords(punctuationPolicy, false);
    }

    public String[] toWords(PunctuationPolicy punctuationPolicy, boolean endText) {
        String text = value.toString();
        Scanner scanner = new Scanner(text);
        ArrayList<String> words = new ArrayList<>();
        scanner.useDelimiter(punctuationPolicy.getDelimiterPattern());
        while (scanner.hasNext()) {
            String[] word = punctuationPolicy.split(scanner.next());
            word[0] = word[0].toLowerCase();
            words.addAll(Arrays.asList(word));
        }
        if (endText) {
            words.add(textEndString);
        }
        return words.toArray(new String[]{});
    }

    public static int maxLength(Text[] texts, PunctuationPolicy punctuationPolicy, boolean endText) {
        int maxLength = 0;
        for (Text text : texts) {
            maxLength = Math.max(maxLength, text.toWords(punctuationPolicy, endText).length);
        }
        return maxLength;
    }

    public Text(String[] words) {
        this.value = new StringBuilder();
        for (String word : words) {
            if (!(word.matches("\\p{Punct}"))) {
                value.append(" ");
            }
            value.append(word);
        }
    }
}
