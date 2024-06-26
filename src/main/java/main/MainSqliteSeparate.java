package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import entry.Entry;
import entry.EntryDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wiktionary.WiktionarySqliteSeparate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** This creates a separated sqlite database of the dictionary
 */
public class MainSqliteSeparate {
    public static final Logger logger = LoggerFactory.getLogger(MainSqliteSeparate.class);

    public static void main(String[] args) {
        System.out.println("========================STARTED========================");
        commenceOperation();
        System.out.println("========================FINISHED========================");
    }

    public static ArrayList<Entry> parseWiktionaryJsonToArray(String filePath) {
        ArrayList<Entry> entries = new ArrayList<>();
        try (InputStream inputStream = new FileInputStream(filePath);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonStreamParser parser = new JsonStreamParser(reader);
            Gson gsonEntry = new GsonBuilder().registerTypeAdapter(Entry.class, new EntryDeserializer()).create();

            while (parser.hasNext()) {
                JsonElement jsonElement = parser.next();
                Entry entry = gsonEntry.fromJson(jsonElement, Entry.class);
                if (!entry.definitions().isEmpty()) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            logger.error(e.toString());
        }

        return entries;
    }

    public static Object[] getEntryList(ArrayList<Entry> entries) {
        //sorting entries
        entries.sort((entry1, entry2) -> {
            String word1 = entry1.word();
            String word2 = entry2.word();

            if (word1.equalsIgnoreCase(word2)) {
                if (word1.toLowerCase().equals(word2)) {
                    if (entry1.partOfSpeech().equals("noun")) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    if (entry1.partOfSpeech().equals("noun")) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            } else {
                return word1.compareToIgnoreCase(word2);
            }
        });

        ArrayList<String> entry_words = new ArrayList<>();
        entries.forEach(entry -> entry_words.add(entry.word()));

        //dividing entries into two at letter "m"
        int index_L = entry_words.indexOf("m");
        ArrayList<Entry> entries_less_equal_to_L = new ArrayList<>(entries.subList(0, index_L));
        ArrayList<Entry> entries_greater_than_L = new ArrayList<>(entries.subList(index_L, entry_words.size()));
        ArrayList<String> entry_words_distinct = new ArrayList<>(entry_words.stream().distinct().toList());

        return new Object[]{entry_words_distinct, entries_less_equal_to_L, entries_greater_than_L};
    }

    @SuppressWarnings("unchecked")
    public static void commenceOperation() {
        String wiktionaryJsonPath = "src/main/resources/kaikki.org-dictionary-English.json";

        //obtaining entries
        ArrayList<Entry> entries = parseWiktionaryJsonToArray(wiktionaryJsonPath);
        Object[] entryList = getEntryList(entries);
        ArrayList<String> entryWords = (ArrayList<String>) entryList[0];
        ArrayList<Entry> entries_less_equal_to_L = (ArrayList<Entry>) entryList[1];
        ArrayList<Entry> entries_greater_than_L = (ArrayList<Entry>) entryList[2];

        //transporting entries to database
        WiktionarySqliteSeparate.setDatabaseName("WiktionaryDatabaseSeparated.db");
        WiktionarySqliteSeparate.createDatabase();
        WiktionarySqliteSeparate.createTables();
        WiktionarySqliteSeparate.insertIntoTables(entries_less_equal_to_L, entries_greater_than_L, entryWords);
        WiktionarySqliteSeparate.createIndices();
    }
}
