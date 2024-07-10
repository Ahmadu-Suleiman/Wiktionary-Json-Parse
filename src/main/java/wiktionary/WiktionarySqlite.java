package wiktionary;

import com.google.gson.Gson;
import entry.Entry;
import main.MainSqliteSeparate;

import java.sql.*;
import java.util.ArrayList;

public class WiktionarySqlite {

    private static String databaseName = null;

    public static void setDatabaseName(String databaseName) {
        WiktionarySqlite.databaseName = databaseName;
    }

    private static Connection getConnection() throws SQLException {
        String path = "jdbc:sqlite:src/main/resources/" + databaseName;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }

        return DriverManager.getConnection(path);
    }

    public static void createDatabase() {
        try (Connection connection = getConnection()) {
            if (connection != null) {
                DatabaseMetaData metaData = connection.getMetaData();
                System.out.println("The driver name is " + metaData.getDriverName());
                System.out.println("A new database has been created: " + databaseName);
            }
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }

    public static void createTables() {
        String entries = "CREATE TABLE entries (" +
                "id INTEGER NOT NULL, word TEXT COLLATE NOCASE, " +
                "plural TEXT, tenses TEXT, compare TEXT, " +
                "part_of_speech TEXT, etymology TEXT, definitions TEXT, " +
                "examples TEXT, synonyms TEXT, antonyms TEXT, hypernyms TEXT, " +
                "hyponyms TEXT, homophones TEXT, PRIMARY KEY(id  AUTOINCREMENT) )";

        String entry_words = "CREATE TABLE entry_words (id INTEGER NOT NULL, " +
                "word TEXT COLLATE NOCASE, PRIMARY KEY(id AUTOINCREMENT))";

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
        String sql = "INSERT INTO entries(word, plural, tenses, compare, part_of_speech, " +
                "etymology, definitions, examples, synonyms, antonyms, hypernyms, " +
                "hyponyms, homophones) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

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
