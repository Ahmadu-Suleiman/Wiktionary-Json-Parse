package entry;

import java.util.ArrayList;
import java.util.Objects;

public class Entry {
    private final String word;
    private final String partOfSpeech;
    private final String plural;
    private final String tenses;
    private final String compare;
    private final ArrayList<String> definitions;
    private final ArrayList<String> synonyms;
    private final ArrayList<String> antonyms;
    private final ArrayList<String> hypernyms;
    private final ArrayList<String> hyponyms;
    private final ArrayList<String> homophones;

    public Entry(String word, String partOfSpeech, String plural,
                 String tenses, String compare, ArrayList<String> definitions,
                 ArrayList<String> synonyms, ArrayList<String> antonyms, ArrayList<String> hypernyms,
                 ArrayList<String> hyponyms, ArrayList<String> homophones) {
        this.word = word;
        this.partOfSpeech = partOfSpeech;
        this.plural = plural;
        this.tenses = tenses;
        this.compare = compare;
        this.definitions = definitions;
        this.synonyms = synonyms;
        this.antonyms = antonyms;
        this.hypernyms = hypernyms;
        this.hyponyms = hyponyms;
        this.homophones = homophones;
    }

    public String getWord() {
        return word;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public String getPlural() {
        return plural;
    }

    public String getTenses() {
        return tenses;
    }

    public String getCompare() {
        return compare;
    }

    public ArrayList<String> getDefinitions() {
        return definitions;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public ArrayList<String> getAntonyms() {
        return antonyms;
    }

    public ArrayList<String> getHypernyms() {
        return hypernyms;
    }

    public ArrayList<String> getHyponyms() {
        return hyponyms;
    }

    public ArrayList<String> getHomophones() {
        return homophones;
    }

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
