package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import entry.Entry;
import entry.EntryDeserializer;
import org.jooq.lambda.Seq;
import wiktionary.WiktionarySqlite;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * This creates an sqlite database of the dictionary
 */
public class MainSqlite {
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
            MainSqliteSeparate.logger.error(e.toString());
        }

        return entries;
    }

    public static Object[] getEntryList(ArrayList<Entry> entries) {
        //sorting entries
        entries.sort((entry1, entry2) -> {
            String word1 = entry1.word();
            String word2 = entry2.word();

            if (word1.equalsIgnoreCase(word2)) {
                if (word1.equals(word2)) {
                    if (entry1.partOfSpeech().equals("Noun")) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    return word2.compareTo(word1);
                }
            } else {
                return word1.compareToIgnoreCase(word2);
            }
        });

        ArrayList<String> entry_words = entries.stream().map(Entry::word).distinct().collect(Collectors.toCollection(ArrayList::new));
        entry_words= (ArrayList<String>) Seq.seq(entry_words).distinct(String::toLowerCase).toList();
        return new Object[]{entry_words, entries};
    }

    @SuppressWarnings("unchecked")
    public static void commenceOperation() {
        String wiktionaryJsonPath = "src/main/resources/kaikki.org-dictionary-English.json";

        //obtaining entries
        ArrayList<Entry> entries = parseWiktionaryJsonToArray(wiktionaryJsonPath);
        Object[] entryList = getEntryList(entries);
        ArrayList<String> entryWords = (ArrayList<String>) entryList[0];
        entries = (ArrayList<Entry>) entryList[1];

        //transporting entries to database
        WiktionarySqlite.setDatabaseName("WiktionaryDatabase.db");
        WiktionarySqlite.createDatabase();
        WiktionarySqlite.createTables();
        WiktionarySqlite.insertIntoTables(entries, entryWords);
        WiktionarySqlite.createIndices();
    }
}
