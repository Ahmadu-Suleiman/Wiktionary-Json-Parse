import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Main {
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
                if (entry.getDefinitions().size() > 0) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

        //dividing entries into two at letter "m"
        int index_L = entry_words.indexOf("m");
        ArrayList<Entry> entries_less_equal_to_L = new ArrayList<>(entries.subList(0, index_L));
        ArrayList<Entry> entries_greater_than_L = new ArrayList<>(entries.subList(index_L, entry_words.size()));
        ArrayList<String> entry_words_distinct = new ArrayList<>(entry_words.stream().distinct().toList());

        return new Object[]{entry_words_distinct, entries_less_equal_to_L, entries_greater_than_L};
    }

    @SuppressWarnings("unchecked")
    public static void commenceOperation() {// TODO: 11/2/2023 MAKE SURE TO CHANGE RAR EXTENSION TO JSON`
        String wiktionaryJsonPath = "C:\\Users\\AHMADU\\Desktop\\files\\My Apps\\Wiktionary Json Parse\\src\\files\\kaikki.org-dictionary-English.rar";

        //obtaining entries
        ArrayList<Entry> entries = parseWiktionaryJsonToArray(wiktionaryJsonPath);
        Object[] entryList = getEntryList(entries);
        ArrayList<String> entryWords = (ArrayList<String>) entryList[0];
        ArrayList<Entry> entries_less_equal_to_L = (ArrayList<Entry>) entryList[1];
        ArrayList<Entry> entries_greater_than_L = (ArrayList<Entry>) entryList[2];

        //transporting entries to database
        WiktionarySqlite.setDatabaseName("WiktionaryDatabase.db");
        WiktionarySqlite.createDatabase();
        WiktionarySqlite.createTables();
        WiktionarySqlite.insertIntoTables(entries_less_equal_to_L, entries_greater_than_L, entryWords);
        WiktionarySqlite.createIndices();
    }
}
