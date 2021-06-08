import java.math.BigInteger;
import java.security.MessageDigest;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;

public class LocalDatabase {

    private final Path dbfolder;
    private final Path dbpath;
    private final String connectionString;
    public FishingUser currentUser = null;

    public LocalDatabase() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        dbfolder = Paths.get(System.getenv("APPDATA"), "FishingCatchesLogger");
        dbpath = Paths.get(dbfolder.toString(), "localDatabase.db");
        connectionString = String.format("jdbc:sqlite:%s", dbpath);

        if (Files.notExists(dbfolder)) {
            try {
                Files.createDirectory(dbfolder);
                createDatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createDatabase() throws SQLException {
        System.out.println("Building new database at: " + dbpath);

        Connection connection = DriverManager.getConnection(connectionString);
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "CREATE TABLE users (" +
                        "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT NOT NULL," +
                        "email TEXT NOT NULL," +
                        "passwordHash TEXT NOT NULL" +
                    ");"
        );
        statement.executeUpdate(
                "CREATE TABLE records (" +
                        "record_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_id INT," +
                        "weight REAL NOT NULL," +
                        "datetime TEXT NOT NULL," + //SQLite does not have a datetime format
                        "FOREIGN KEY (user_id) REFERENCES users (user_id)" +
                    ");"
        );
        statement.executeUpdate(
                "CREATE TABLE catches (" +
                        "catch_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "record_id INTEGER," +
                        "latitude REAL NOT NULL," +
                        "longitude REAL NOT NULL," +
                        "FOREIGN KEY (record_id) REFERENCES records (record_id)" +
                    ");"
        );
        statement.executeUpdate(
                "CREATE TABLE sells (" +
                        "sell_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "record_id INTEGER," +
                        "revenue REAL NOT NULL," +
                        "FOREIGN KEY (record_id) REFERENCES records (record_id)" +
                    ");"
        );
        statement.close();
        connection.close();
    }

    public void addUser(String username, String email, String plainPassword) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "INSERT INTO users (username, email, passwordHash) VALUES (?, ?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, username);
        statement.setString(2, email);
        statement.setString(3, FishingUser.hashPassword(plainPassword));
        statement.executeUpdate();
        connection.close();
    }

    public FishingUser changeUser(String username, String plainPassword) throws SQLException, UserNotFoundException, IncorrectPasswordException {
        FishingUser user = searchForUser(username, plainPassword);
        currentUser = user;
        return user;
    }

    public FishingUser searchForUser(String username, String plainPassword) throws  SQLException, UserNotFoundException, IncorrectPasswordException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "SELECT * FROM users WHERE username = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, username);
        statement.execute();
        ResultSet results = statement.getResultSet();
        FishingUser user;

        if (results.next()) {
            user = new FishingUser(
                    results.getString(2),
                    results.getString(3),
                    results.getInt(1)
            );
            user.setPasswordHash(results.getString(4));
        } else {
            connection.close();
            throw new UserNotFoundException("A user with the specified username was not found in the database.");
        }

        if (user.getPasswordHash().equals(FishingUser.hashPassword(plainPassword))) {
            connection.close();
            return user;
        } else {
            connection.close();
            throw new IncorrectPasswordException("The incorrect password was provided for the specified user");
        }
    }

    private int generateRecord(LocalDateTime dateTime, double weight) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "INSERT INTO records (user_id, weight, datetime) VALUES (?, ?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        statement.setInt(1, currentUser.getUser_id());
        statement.setDouble(2, weight);
        statement.setString(3, dateTime.toString());
        statement.executeUpdate();

        int pkey;
        ResultSet results = statement.getGeneratedKeys();
        if (results.next()) {
            pkey = results.getInt(1);
        } else {
            throw new SQLException("Couldn't get generated index");
        }
        connection.close();
        return pkey;
    }

    public void inputSellData(LocalDateTime dateTime, double weight, double revenue) throws SQLException {
        int record_id = generateRecord(dateTime, weight);
        Connection connection = DriverManager.getConnection(connectionString);
        String sql = "INSERT INTO sells (record_id, revenue) VALUES (?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, record_id);
        statement.setDouble(2, revenue);
        statement.executeUpdate();
        connection.close();
    }

    public static void main(String[] args) {
        try {
            LocalDatabase db = new LocalDatabase();
//            db.addUser("eden", "gae19jtu@uea.ac.uk", "password");
            System.out.println(db.changeUser("eden", "password"));
            db.inputSellData(LocalDateTime.now(), 420.69, 92.42);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class FishingUser {
        private final String username;
        private final String email;
        private final int user_id;
        private String passwordHash;

        public FishingUser(String username, String email, int user_id) {
            this.username = username;
            this.email = email;
            this.user_id = user_id;
        }

        public FishingUser(String username, String email, int user_id, String plainPassword) {
            this.username = username;
            this.email = email;
            this.user_id = user_id;
            this.passwordHash = hashPassword(plainPassword);
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public String getUsername() {
            return this.username;
        }

        public int getUser_id() {
            return this.user_id;
        }

        public String getEmail() {
            return this.email;
        }

        public String getPasswordHash() {
            return this.passwordHash;
        }

        public static String hashPassword(String password) {
            //https://www.geeksforgeeks.org/md5-hash-in-java/
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");     //md5 is bad, use SHA
                byte[] bytes = messageDigest.digest(password.getBytes());
                BigInteger bigInt = new BigInteger(1, bytes);

                String hash = bigInt.toString(16);
                while (hash.length() < 32) {
                    hash = "0" + hash;
                }
                return hash;

            } catch (java.security.NoSuchAlgorithmException e) {
                System.out.println(e);
                return null;
            }
        }

        public String toString() {
            return String.format("%d\t%s\t%s", user_id, username, email);
        }
    }

    private abstract class Record {
        public LocalDateTime date;
        public double weight;
        protected int record_id;

        public void editDate(LocalDateTime newDate) throws SQLException {
            Connection connection = DriverManager.getConnection(connectionString);
            String sql = "UPDATE records SET datetime = ? WHERE record_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, newDate.toString());
            statement.setInt(2, record_id);
            statement.execute();
            this.date = newDate;
        }

        public void editWeight(double newWeight) throws SQLException {
            Connection connection = DriverManager.getConnection(connectionString);
            String sql = "UPDATE records SET weight = ? WHERE record_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setDouble(1, newWeight);
            statement.setInt(2, record_id);
            statement.execute();
            this.weight = newWeight;
        }
    }

    public class CatchRecord extends Record {
        public double latitude;
        public double longitude;

        public void editLocation(double latitude, double longitude) throws SQLException {
            Connection connection = DriverManager.getConnection(connectionString);
            String sql = "UPDATE catches SET latitude = ? AND longitude = ? WHERE record_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setDouble(1, latitude);
            statement.setDouble(2, longitude);
            statement.setInt(3, record_id);
            statement.execute();
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public class SellRecord extends Record {
        public double price;
    }

    public static class UserNotFoundException extends Exception {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class IncorrectPasswordException extends Exception {
        public IncorrectPasswordException(String message) {
            super(message);
        }
    }
}
