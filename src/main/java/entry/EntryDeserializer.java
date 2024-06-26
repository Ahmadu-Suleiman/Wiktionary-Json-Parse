package entry;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.*;

public class EntryDeserializer implements JsonDeserializer<Entry> {

    public static String removeThesaurus(String word) {
        word = word.replace("[⇒ thesaurus]", "");
        if (word.contains("thesaurus") || word.contains("Thesaurus") || word.isEmpty()) {
            return null;
        }

        return word;
    }

    @Override
    public Entry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String word = jsonObject.get("word").getAsString();
        String partOfSpeech = jsonObject.get("pos").getAsString();
        partOfSpeech = switch (partOfSpeech) {
            case "abbrev" -> "ab";
            case "affix" -> "af";
            case "article" -> "ar";
            case "character" -> "ch";
            case "circumfix" -> "cr";
            case "conj" -> "cn";
            case "det" -> "dt";
            case "infix" -> "inf";
            case "interfix" -> "intf";
            case "intj" -> "int";
            case "name" -> "nm";
            case "noun" -> "n";
            case "particle" -> "prt";
            case "phrase" -> "ph";
            case "postp" -> "pp";
            case "prefix" -> "prf";
            case "prep" -> "prp";
            case "prep_phrase" -> "prpp";
            case "pron" -> "prn";
            case "proverb" -> "prv";
            case "punct" -> "pct";
            case "suffix" -> "sf";
            case "symbol" -> "sm";
            case "verb" -> "v";
            default -> jsonObject.get("pos").getAsString();
        };

        String plural = null;
        String presentSingular = null;
        String presentParticiple = null;
        String pastParticiple = null;
        String past = null;
        String comparative = null;
        String superlative = null;

        if (jsonObject.get("forms") != null) {
            JsonArray forms = jsonObject.get("forms").getAsJsonArray();

            for (JsonElement element : forms) {
                JsonObject formObject = element.getAsJsonObject();

                if (formObject.get("tags") != null) {
                    //plural
                    String tag = formObject.get("tags").getAsJsonArray().get(0).getAsString();
                    if ("plural".equals(tag)) {
                        plural = formObject.get("form").getAsString();
                        break;
                    }

                    //tenses
                    String[] array = new Gson().fromJson(formObject.get("tags").getAsJsonArray(), String[].class);
                    ArrayList<String> tags = new ArrayList<>(List.of(array));

                    if (tags.contains("singular")) {
                        presentSingular = formObject.get("form").getAsString();
                    }

                    if (tags.contains("present") && tags.contains("participle")) {
                        presentParticiple = formObject.get("form").getAsString();
                    }

                    if (tags.contains("past") && tags.contains("participle")) {
                        pastParticiple = formObject.get("form").getAsString();
                    }

                    if (tags.contains("past") && !tags.contains("participle")) {
                        past = formObject.get("form").getAsString();
                        break;
                    }

                    //compare
                    tag = formObject.get("tags").getAsJsonArray().get(0).getAsString();

                    if ("comparative".equals(tag)) {
                        comparative = formObject.get("form").getAsString();
                    }

                    if ("superlative".equals(tag)) {
                        superlative = formObject.get("form").getAsString();
                        break;
                    }
                }
            }
        }

        JsonArray senses = jsonObject.getAsJsonArray("senses");

        ArrayList<String> definitions = new ArrayList<>();
        ArrayList<String> synonyms = new ArrayList<>();
        ArrayList<String> antonyms = new ArrayList<>();
        ArrayList<String> hypernyms = new ArrayList<>();
        ArrayList<String> hyponyms = new ArrayList<>();
        ArrayList<String> homophones = new ArrayList<>();

        for (JsonElement element : senses) {
            JsonObject senseObject = element.getAsJsonObject();

            //definitions and examples
            String definition = senseObject.get("raw_glosses") != null ?
                    senseObject.get("raw_glosses").getAsJsonArray().get(0).getAsString() : null;

            if (definition != null) {

                String example = senseObject.get("examples") != null ?
                        senseObject.get("examples").getAsJsonArray().get(0).getAsJsonObject().get("text").getAsString() : null;

                definition = example != null ? String.format("%s\n\te.g. %s", definition, example) : definition;
                definitions.add(definition);
            }

            //synonyms
            if (senseObject.get("synonyms") != null) {
                JsonArray synonymsArray = senseObject.get("synonyms").getAsJsonArray();
                for (JsonElement elementSense : synonymsArray) {
                    JsonObject synonymObject = elementSense.getAsJsonObject();
                    String synonym = removeThesaurus(synonymObject.get("word").getAsString());
                    if (synonym != null) {
                        synonyms.add(synonym);
                    }
                }
            }

            //antonyms
            if (senseObject.get("antonyms") != null) {
                JsonArray antonymsArray = senseObject.get("antonyms").getAsJsonArray();
                for (JsonElement elementSense : antonymsArray) {
                    JsonObject antonymObject = elementSense.getAsJsonObject();
                    String antonym = removeThesaurus(antonymObject.get("word").getAsString());
                    if (antonym != null) {
                        antonyms.add(antonym);
                    }
                }
            }

            //hypernyms
            if (senseObject.get("hypernyms") != null) {
                JsonArray hypernymsArray = senseObject.get("hypernyms").getAsJsonArray();
                for (JsonElement elementSense : hypernymsArray) {
                    JsonObject hypernymObject = elementSense.getAsJsonObject();
                    String hypernym = removeThesaurus(hypernymObject.get("word").getAsString());
                    if (hypernym != null) {
                        hypernyms.add(hypernym);
                    }
                }
            }

            //hyponyms
            if (senseObject.get("hyponyms") != null) {
                JsonArray hyponymsArray = senseObject.get("hyponyms").getAsJsonArray();
                for (JsonElement elementSense : hyponymsArray) {
                    JsonObject hyponymObject = elementSense.getAsJsonObject();
                    String hyponym = removeThesaurus(hyponymObject.get("word").getAsString());
                    if (hyponym != null) {
                        hyponyms.add(hyponym);
                    }
                }
            }
        }

        //homophones
        if (jsonObject.get("sounds") != null) {
            JsonArray homophonesArray = jsonObject.get("sounds").getAsJsonArray();
            for (JsonElement element : homophonesArray) {
                JsonObject homophoneObject = element.getAsJsonObject();
                if (homophoneObject.get("homophone") != null) {
                    String homophone = homophoneObject.get("homophone").getAsString();
                    homophones.add(homophone);
                }
            }
        }

        partOfSpeech = getPartOfSpeech(partOfSpeech);

        //aggregating attributes
        ArrayList<String> tenses = new ArrayList<>(Arrays.asList(presentSingular, presentParticiple, pastParticiple, past));
        tenses.removeIf(Objects::isNull);
        ArrayList<String> compare = new ArrayList<>(Arrays.asList(word, comparative, superlative));
        compare.removeIf(Objects::isNull);

        if (compare.size() == 1) {
            compare = new ArrayList<>();
        }

        //sorting lists
        Collections.sort(synonyms);
        Collections.sort(antonyms);
        Collections.sort(hypernyms);
        Collections.sort(hyponyms);
        Collections.sort(homophones);

        //removing duplicates
        synonyms = new ArrayList<>(synonyms.stream().distinct().toList());
        antonyms = new ArrayList<>(antonyms.stream().distinct().toList());
        hypernyms = new ArrayList<>(hypernyms.stream().distinct().toList());
        hyponyms = new ArrayList<>(hyponyms.stream().distinct().toList());
        homophones = new ArrayList<>(homophones.stream().distinct().toList());

        return new Entry(word, partOfSpeech, plural, tenses, compare, definitions,
                synonyms, antonyms, hypernyms, hyponyms, homophones);
    }

    public static String getPartOfSpeech(String abbreviation) {
        return switch (abbreviation) {
            case "n" -> "Noun";
            case "prp" -> "Preposition";
            case "adj" -> "Adjective";
            case "adv" -> "Adverb";
            case "prn" -> "Pronoun";
            case "v" -> "Verb";
            case "cn" -> "Conjunction";
            case "int" -> "Interjection";
            case "pct" -> "Punctuation";
            case "prt" -> "Particle";
            case "ar" -> "Article";
            case "dt" -> "Determiner";
            case "prv" -> "Proverb";
            case "sf" -> "Suffix";
            case "prf" -> "Prefix";
            case "intf" -> "Interfix";
            case "inf" -> "Infix";
            case "sm" -> "Symbol";
            case "ph" -> "Phrase";
            case "ab" -> "Abbreviation";
            case "af" -> "Affix";
            case "ch" -> "Character";
            case "cr" -> "Circumfix";
            case "nm" -> "Name";
            case "num" -> "Numeral";
            case "pp" -> "Postposition";
            case "prpp" -> "Prepositional phrase";
            default -> abbreviation;
        };
    }
}
