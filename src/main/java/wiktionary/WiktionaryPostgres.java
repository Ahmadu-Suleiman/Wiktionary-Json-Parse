package wiktionary;

import com.google.gson.Gson;
import entry.Entry;
import main.MainSqliteSeparate;

import java.sql.*;
import java.util.ArrayList;

public class WiktionaryPostgres {

    private static String connectionUri = null;

    public static void setConnectionUri(String connectionUri) {
        WiktionaryPostgres.connectionUri = connectionUri;
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionUri);
    }

    public static void dropExistingTablesAndIndexes() {
        String dropEntries = "DROP TABLE IF EXISTS entries";
        String dropEntryWords = "DROP TABLE IF EXISTS entry_words";

        String dropIndexEntries = "DROP INDEX IF EXISTS index_entries";
        String dropIndexEntryWords = "DROP INDEX IF EXISTS index_entry_word";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(dropEntries);
            statement.execute(dropEntryWords);
            statement.execute(dropIndexEntries);
            statement.execute(dropIndexEntryWords);

            System.out.println("Tables and indexes dropped successfully!");
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }
    public static void createTables() {
        String entries = """
                CREATE TABLE IF NOT EXISTS entries (
                    id SERIAL PRIMARY KEY,
                    word TEXT,
                    plural TEXT,
                    tenses TEXT,
                    compare TEXT,
                    part_of_speech TEXT,
                    etymology TEXT,
                    definitions TEXT,
                    examples TEXT,
                    synonyms TEXT,
                    antonyms TEXT,
                    hypernyms TEXT,
                    hyponyms TEXT,
                    homophones TEXT
                )""";

        String entry_words = """
                CREATE TABLE IF NOT EXISTS entry_words (id SERIAL PRIMARY KEY, word TEXT)""";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(entries);
            statement.execute(entry_words);

            System.out.println("Tables created successfully!");
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }

    public static void insertIntoTables(ArrayList<Entry> entries, ArrayList<String> entry_words) {
        String sql = "INSERT INTO entries (word, plural, tenses, compare, part_of_speech, " +
                "etymology, definitions, examples, synonyms, antonyms, hypernyms, " +
                "hyponyms, homophones) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

        insertEntries(entries, sql);

        sql = "INSERT INTO entry_words (entry) VALUES(?)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (String word : entry_words) {
                statement.setString(1, word);
                statement.addBatch();
                System.out.println(word);
            }

            int[] result = statement.executeBatch();
            System.out.printf("Inserted %d number of rows%n", result.length);
            connection.commit();
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }

    public static void setEntryStatement(Entry entry, PreparedStatement statement) throws SQLException {
        Gson gson = new Gson();
        statement.setString(1, entry.word());
        statement.setString(2, entry.plural());
        statement.setString(3, gson.toJson(entry.tenses()));
        statement.setString(4, gson.toJson(entry.compare()));
        statement.setString(5, entry.partOfSpeech());
        statement.setString(6, entry.etymology());
        statement.setString(7, gson.toJson(entry.definitions()));
        statement.setString(8, gson.toJson(entry.examples()));
        statement.setString(9, gson.toJson(entry.synonyms()));
        statement.setString(10, gson.toJson(entry.antonyms()));
        statement.setString(11, gson.toJson(entry.hypernyms()));
        statement.setString(12, gson.toJson(entry.hyponyms()));
        statement.setString(13, gson.toJson(entry.homophones()));
        statement.addBatch();
        System.out.println(entry.word());
    }

    public static void insertEntries(ArrayList<Entry> entries, String sql) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (Entry entry : entries) {
                setEntryStatement(entry, statement);
            }
            int result = statement.executeBatch().length;
            System.out.printf("Inserted %d number of rows%n", result);
            connection.commit();
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }

    public static void createIndices() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            String sql = "CREATE INDEX index_entry_word ON entry_words (word)";
            statement.execute(sql);

            sql = "CREATE INDEX index_entries ON entries (word)";
            statement.execute(sql);

            System.out.println("Indices created successfully!");
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }
}
