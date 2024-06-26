package wiktionary;

import com.google.gson.Gson;
import entry.Entry;
import main.MainSqliteSeparate;

import java.sql.*;
import java.util.ArrayList;

public class WiktionarySqliteSeparate {

    private static String databaseName = null;

    public static void setDatabaseName(String databaseName) {
        WiktionarySqliteSeparate.databaseName = databaseName;
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
        String entries_greater_than_L = "CREATE TABLE entries_greater_than_L (" +
                "entry_id INTEGER NOT NULL, entry_word TEXT COLLATE NOCASE, " +
                "entry_plural TEXT, entry_tenses TEXT, entry_compare TEXT, " +
                "entry_part_of_speech TEXT, entry_definitions TEXT, " +
                "entry_synonyms TEXT, entry_antonyms TEXT, entry_hypernyms TEXT, " +
                "entry_hyponyms TEXT, entry_homophones TEXT, PRIMARY KEY(entry_id  AUTOINCREMENT) )";

        String entries_less_equal_to_L = "CREATE TABLE entries_less_equal_to_L (" +
                "entry_id INTEGER NOT NULL, entry_word TEXT COLLATE NOCASE, " +
                "entry_plural TEXT, entry_tenses TEXT, entry_compare TEXT, " +
                "entry_part_of_speech TEXT, entry_definitions TEXT, " +
                "entry_synonyms TEXT, entry_antonyms TEXT, entry_hypernyms TEXT, " +
                "entry_hyponyms TEXT, entry_homophones TEXT, PRIMARY KEY(entry_id  AUTOINCREMENT) )";

        String entry_words = "CREATE TABLE entry_words (entry_id INTEGER NOT NULL, " +
                "entry_word TEXT COLLATE NOCASE, PRIMARY KEY(entry_id  AUTOINCREMENT) )";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(entries_greater_than_L);
            statement.executeUpdate(entries_less_equal_to_L);
            statement.executeUpdate(entry_words);

            System.out.println("Tables created successfully!");
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }

    public static void insertIntoTables(ArrayList<Entry> entries_less_equal_to_L,
                                        ArrayList<Entry> entries_greater_than_L, ArrayList<String> entry_words) {
        String sql = "INSERT INTO entries_less_equal_to_L(entry_word, entry_plural, entry_tenses, entry_compare," +
                "entry_part_of_speech, entry_definitions, entry_synonyms, entry_antonyms, entry_hypernyms, " +
                "entry_hyponyms, entry_homophones) VALUES(?,?,?,?,?,?,?,?,?,?,?)";

        insertEntries(entries_less_equal_to_L, sql);

        sql = "INSERT INTO entries_greater_than_L(entry_word, entry_plural, entry_tenses, entry_compare," +
                "entry_part_of_speech, entry_definitions, entry_synonyms, entry_antonyms, entry_hypernyms, " +
                "entry_hyponyms, entry_homophones) VALUES(?,?,?,?,?,?,?,?,?,?,?)";

        insertEntries(entries_greater_than_L, sql);

        sql = "INSERT INTO entry_words(entry_word) VALUES(?)";

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
        statement.setString(6, gson.toJson(entry.definitions()));
        statement.setString(7, gson.toJson(entry.synonyms()));
        statement.setString(8, gson.toJson(entry.antonyms()));
        statement.setString(9, gson.toJson(entry.hypernyms()));
        statement.setString(10, gson.toJson(entry.hyponyms()));
        statement.setString(11, gson.toJson(entry.homophones()));
        statement.addBatch();
        System.out.println(entry.word());
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
            String sql = "CREATE INDEX index_entry_word ON entry_words(entry_word)";
            statement.execute(sql);

            sql = "CREATE INDEX index_greater_than_L ON entries_greater_than_L(entry_word)";
            statement.execute(sql);

            sql = "CREATE INDEX index_less_equal_to_L ON entries_less_equal_to_L(entry_word)";
            statement.execute(sql);

            System.out.println("Indices created successfully!");
        } catch (SQLException e) {
            MainSqliteSeparate.logger.error(e.toString());
        }
    }
}
