package entry;

import java.util.ArrayList;
import java.util.Objects;

public record Entry(String word, String partOfSpeech, String etymology, String plural, ArrayList<String> tenses,
                    ArrayList<String> compare, ArrayList<String> definitions, ArrayList<String> examples, ArrayList<String> synonyms,
                    ArrayList<String> antonyms, ArrayList<String> hypernyms, ArrayList<String> hyponyms,
                    ArrayList<String> homophones) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return Objects.equals(word, entry.word) && Objects.equals(plural, entry.plural) &&
                Objects.equals(etymology, entry.etymology) && Objects.equals(partOfSpeech, entry.partOfSpeech) &&
                Objects.equals(tenses, entry.tenses) && Objects.equals(compare, entry.compare) &&
                Objects.equals(examples, entry.examples) && Objects.equals(synonyms, entry.synonyms) &&
                Objects.equals(antonyms, entry.antonyms) && Objects.equals(hyponyms, entry.hyponyms) &&
                Objects.equals(hypernyms, entry.hypernyms) && Objects.equals(homophones, entry.homophones) &&
                Objects.equals(definitions, entry.definitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, partOfSpeech, etymology, plural, tenses, compare, definitions, examples, synonyms,
                antonyms, hypernyms, hyponyms, homophones);
    }
}
