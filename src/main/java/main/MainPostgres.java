package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import entry.Entry;
import entry.EntryDeserializer;
import wiktionary.WiktionaryPostgres;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainPostgres {
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
                if (!entry.getDefinitions().isEmpty()) {
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
            String word1 = entry1.getWord();
            String word2 = entry2.getWord();

            if (word1.equalsIgnoreCase(word2)) {
                if (word1.toLowerCase().equals(word2)) {
                    if (entry1.getPartOfSpeech().equals("n")) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    if (entry1.getPartOfSpeech().equals("n")) {
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
        entries.forEach(entry -> entry_words.add(entry.getWord()));
        ArrayList<String> entry_words_distinct = new ArrayList<>(entry_words.stream().distinct().toList());

        return new Object[]{entry_words_distinct, entries};
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
        WiktionaryPostgres.setDatabaseName("WiktionaryDatabase",
                "postgres","tachyon");
        WiktionaryPostgres.createDatabase();
        WiktionaryPostgres.createTables();
        WiktionaryPostgres.insertIntoTables(entries, entryWords);
        WiktionaryPostgres.createIndices();
    }
}
