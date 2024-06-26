package entry;

import java.util.ArrayList;
import java.util.Objects;

public record Entry(String word, String partOfSpeech, String plural, ArrayList<String> tenses,
                    ArrayList<String> compare, ArrayList<String> definitions, ArrayList<String> synonyms,
                    ArrayList<String> antonyms, ArrayList<String> hypernyms, ArrayList<String> hyponyms,
                    ArrayList<String> homophones) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return word.equals(entry.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word);
    }
}
