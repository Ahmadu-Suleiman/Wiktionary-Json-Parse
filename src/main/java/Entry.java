import java.util.ArrayList;
import java.util.Objects;

public class Entry {
    private String word;
    private String partOfSpeech;
    private String plural;
    private String tenses;
    private String compare;
    private ArrayList<String> definitions;
    private ArrayList<String> synonyms;
    private ArrayList<String> antonyms;
    private ArrayList<String> hypernyms;
    private ArrayList<String> hyponyms;
    private ArrayList<String> homophones;

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

    public void setWord(String word) {
        this.word = word;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public String getPlural() {
        return plural;
    }

    public void setPlural(String plural) {
        this.plural = plural;
    }

    public String getTenses() {
        return tenses;
    }

    public void setTenses(String tenses) {
        this.tenses = tenses;
    }

    public String getCompare() {
        return compare;
    }

    public void setCompare(String compare) {
        this.compare = compare;
    }

    public ArrayList<String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(ArrayList<String> definitions) {
        this.definitions = definitions;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public ArrayList<String> getAntonyms() {
        return antonyms;
    }

    public void setAntonyms(ArrayList<String> antonyms) {
        this.antonyms = antonyms;
    }

    public ArrayList<String> getHypernyms() {
        return hypernyms;
    }

    public void setHypernyms(ArrayList<String> hypernyms) {
        this.hypernyms = hypernyms;
    }

    public ArrayList<String> getHyponyms() {
        return hyponyms;
    }

    public void setHyponyms(ArrayList<String> hyponyms) {
        this.hyponyms = hyponyms;
    }

    public ArrayList<String> getHomophones() {
        return homophones;
    }

    public void setHomophones(ArrayList<String> homophones) {
        this.homophones = homophones;
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
