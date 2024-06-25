package wiktionary;

import com.google.gson.Gson;
import entry.Entry;
import main.MainSqliteSeparate;

import java.sql.*;
import java.util.ArrayList;

public class WiktionaryPostgres {

    private static String databaseName = null;
    private static String user = null;
    private static String password = null;

    public static void setDatabaseName(String databaseName, String user, String password) {
        WiktionaryPostgres.databaseName = databaseName;
        WiktionaryPostgres.user = user;
        WiktionaryPostgres.password = password;
    }

    private static Connection getConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/" + databaseName;
        return DriverManager.getConnection(url, user, password);
    }

    public static void createTables() {
        String entries = "CREATE TABLE IF NOT EXISTS entries (" +
                "id SERIAL PRIMARY KEY, " +
                "word TEXT, " +
                "plural TEXT, " +
                "tenses TEXT, " +
                "compare TEXT, " +
                "part_of_speech TEXT, " +
                "definitions TEXT, " +
                "synonyms TEXT, " +
                "antonyms TEXT, " +
                "hypernyms TEXT, " +
                "hyponyms TEXT, " +
                "entry_homophones TEXT)";

        String entry_words = "CREATE TABLE IF NOT EXISTS entry_words (id SERIAL NOT NULL, " +
                "word TEXT, PRIMARY KEY(id))";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(entries);
            statement.executeUpdate(entry_words);

            System.out.println("Tables created successfully!");
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }

    public static void insertIntoTables(ArrayList<Entry> entries, ArrayList<String> entry_words) {
        String sql = "INSERT INTO entries(word, plural, tenses, compare," +
                "part_of_speech, definitions, synonyms, antonyms, hypernyms, " +
                "hyponyms, homophones) VALUES(?,?,?,?,?,?,?,?,?,?,?)";

        insertEntries(entries, sql);

        sql = "INSERT INTO entry_words(word) VALUES(?)";

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
        statement.setString(1, entry.getWord());
        statement.setString(2, entry.getPlural());
        statement.setString(3, entry.getTenses());
        statement.setString(4, entry.getCompare());
        statement.setString(5, entry.getPartOfSpeech());
        statement.setString(6, gson.toJson(entry.getDefinitions()));
        statement.setString(7, gson.toJson(entry.getSynonyms()));
        statement.setString(8, gson.toJson(entry.getAntonyms()));
        statement.setString(9, gson.toJson(entry.getHypernyms()));
        statement.setString(10, gson.toJson(entry.getHyponyms()));
        statement.setString(11, gson.toJson(entry.getHomophones()));
        statement.addBatch();
        System.out.println(entry.getWord());
    }

    public static void insertEntries(ArrayList<Entry> entries, String sql) {
        int middle = Math.floorDiv(entries.size(), 2);
        ArrayList<Entry> entriesA = new ArrayList<>(entries.subList(0, middle));
        ArrayList<Entry> entriesB = new ArrayList<>(entries.subList(middle, entries.size()));

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (Entry entry : entriesA) {
                setEntryStatement(entry, statement);
            }
            int result = statement.executeBatch().length;

            for (Entry entry : entriesB) {
                setEntryStatement(entry, statement);
            }

            result += statement.executeBatch().length;
            System.out.printf("Inserted %d number of rows%n", result);
            connection.commit();
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }

    public static void createIndices() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            String sql = "CREATE INDEX index_entry_word ON entry_words(word)";
            statement.execute(sql);

            sql = "CREATE INDEX index_entries ON entries(word)";
            statement.execute(sql);

            System.out.println("Indices created successfully!");
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }
}
